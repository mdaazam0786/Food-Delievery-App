package com.foodzie.restaurant_acceptance_service.listener;

import com.foodzie.restaurant_acceptance_service.event.OrderStatusUpdatedEvent;
import com.foodzie.restaurant_acceptance_service.service.OrderAcceptanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the "order-events" topic and routes relevant events to OrderAcceptanceService.
 *
 * This service only cares about one specific transition:
 *   triggerReason = PAYMENT_SUCCESSFUL — meaning the user has paid and the restaurant
 *   needs to accept or decline the order.
 *
 * All other event types on this topic (ORDER_CREATED, status updates from other services)
 * are silently ignored.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsListener {

    private final OrderAcceptanceService acceptanceService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderStatusListenerContainerFactory"
    )
    public void onOrderEvent(OrderStatusUpdatedEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null or malformed OrderStatusUpdatedEvent — skipping");
            return;
        }

        // Only act on the PAYMENT_SUCCESSFUL trigger — this is the moment the
        // restaurant needs to be notified of a new paid order.
        if (!"PAYMENT_SUCCESSFUL".equals(event.getPayload().getTriggerReason())) {
            return;
        }

        log.info("New paid order received: orderId={} restaurantId={}",
                event.getPayload().getOrderId(), event.getPayload().getRestaurantId());

        acceptanceService.handleNewPaidOrder(event);
    }
}
