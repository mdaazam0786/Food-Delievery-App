package com.foodzie.delivery_acceptance_service.listener;

import com.foodzie.delivery_acceptance_service.event.DeliveryMatchingEvent;
import com.foodzie.delivery_acceptance_service.service.OfferBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes DELIVERY_MATCHING_INITIATED events from the "delivery-events" topic.
 *
 * Published by: delivery-matching-service
 * Payload:      orderId, restaurantId, cityZone, candidateDriverIds (top 5-10)
 *
 * On receipt, delegates to OfferBroadcastService which:
 *   1. Stores the offer in Redis with a 30s TTL
 *   2. Pushes the offer to each candidate driver via SSE
 *
 * NO_DRIVERS_AVAILABLE events are ignored here — the notification-service
 * handles those to alert the customer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryMatchingEventListener {

    private final OfferBroadcastService broadcastService;

    @KafkaListener(
            topics = "${app.kafka.topics.delivery-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "deliveryMatchingListenerContainerFactory"
    )
    public void onDeliveryMatchingEvent(DeliveryMatchingEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null or malformed DeliveryMatchingEvent — skipping");
            return;
        }

        String eventType = event.getEventType();
        String orderId   = event.getPayload().getOrderId();

        if (!"DELIVERY_MATCHING_INITIATED".equals(eventType)) {
            log.debug("Ignoring event type={} for orderId={}", eventType, orderId);
            return;
        }

        log.info("DeliveryMatchingEvent received: orderId={} candidates={}",
                orderId, event.getPayload().getCandidateDriverIds().size());

        broadcastService.broadcastOffer(event);
    }
}
