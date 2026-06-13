package com.foodzie.restaurant_acceptance_service.service;

import com.foodzie.restaurant_acceptance_service.event.RestaurantDecisionEvent;
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
 * When a pending_order:{orderId} key expires (TTL = 5 min), this listener fires a
 * RestaurantDecisionEvent with newStatus = DECLINED and triggerReason = TIMEOUT so the
 * order-service can cancel the order and trigger a refund.
 *
 * Redis must have keyspace notifications enabled:
 *   redis-cli CONFIG SET notify-keyspace-events Ex
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisExpiryListener implements MessageListener {

    private static final String PENDING_ORDER_PREFIX = "pending_order:";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    /**
     * Called by the Redis message listener container when any key expires.
     * We filter for keys that start with pending_order:.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());
        onKeyExpired(expiredKey);
    }

    public void onKeyExpired(String expiredKey) {
        if (!expiredKey.startsWith(PENDING_ORDER_PREFIX)) {
            return; // Not our key — ignore
        }

        String orderId = expiredKey.substring(PENDING_ORDER_PREFIX.length());
        log.warn("Pending order expired (5-min timeout): orderId={}", orderId);

        // We no longer have the full PendingOrder (it's expired), so we publish
        // a minimal event. The order-service will look up the full record by orderId.
        RestaurantDecisionEvent event = RestaurantDecisionEvent.builder()
                .payload(RestaurantDecisionEvent.Payload.builder()
                        .orderId(orderId)
                        .previousStatus("PREPARING")
                        .newStatus("DECLINED")
                        .triggerReason("TIMEOUT")
                        .metadata(RestaurantDecisionEvent.DecisionMetadata.builder()
                                .declineReason("Restaurant did not respond within 5 minutes")
                                .build())
                        .build())
                .build();

        kafkaTemplate.send(orderEventsTopic, orderId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish timeout event for orderId={}: {}", orderId, ex.getMessage());
                    } else {
                        log.info("Timeout event published for orderId={}", orderId);
                    }
                });
    }
}
