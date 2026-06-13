package com.foodzie.delivery_acceptance_service.service;

import com.foodzie.delivery_acceptance_service.event.OfferExpiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Listens for Redis keyspace expiry events.
 *
 * When an offer:pending:{orderId} key expires (TTL = 30s with no acceptance),
 * this listener fires an OfferExpiredEvent so the delivery-matching-service
 * can widen the search radius and re-broadcast to a larger pool of drivers.
 *
 * Redis must have keyspace notifications enabled:
 *   redis-cli CONFIG SET notify-keyspace-events Ex
 *
 * The key format is: offer:pending:{orderId}
 * Value format:      {restaurantId}:{cityZone}:{previousRadiusKm}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOfferExpiryListener implements MessageListener {

    private static final String OFFER_KEY_PREFIX = "offer:pending:";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.delivery-events}")
    private String deliveryEventsTopic;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());
        onKeyExpired(expiredKey);
    }

    public void onKeyExpired(String expiredKey) {
        if (!expiredKey.startsWith(OFFER_KEY_PREFIX)) {
            return; // Not our key — ignore
        }

        String orderId = expiredKey.substring(OFFER_KEY_PREFIX.length());
        log.warn("Delivery offer expired (30s timeout): orderId={}", orderId);

        // The value is gone (key expired), so we publish a minimal event.
        // The delivery-matching-service will look up the full order context by orderId.
        OfferExpiredEvent event = OfferExpiredEvent.builder()
                .payload(OfferExpiredEvent.Payload.builder()
                        .orderId(orderId)
                        .reason("NO_DRIVER_ACCEPTED_WITHIN_30S")
                        .build())
                .build();

        kafkaTemplate.send(deliveryEventsTopic, orderId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OfferExpiredEvent for orderId={}: {}",
                                orderId, ex.getMessage());
                    } else {
                        log.info("OfferExpiredEvent published for orderId={}", orderId);
                    }
                });
    }
}
