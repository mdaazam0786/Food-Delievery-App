package com.foodzie.websocket_manager.listener;

import com.foodzie.websocket_manager.event.OrderStatusUpdatedEvent;
import com.foodzie.websocket_manager.service.ActiveDeliveryRegistry;
import com.foodzie.websocket_manager.service.TrackingSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes ORDER_STATUS_UPDATED events from the "order-events" topic.
 *
 * Acts on two transitions:
 *
 *   OUT_FOR_DELIVERY:
 *     - Registers driverId → orderId in ActiveDeliveryRegistry.
 *     - From this point, every GPS ping from this driver is forwarded to the
 *       customer's WebSocket session.
 *
 *   DELIVERED:
 *     - Deregisters the driver from ActiveDeliveryRegistry (stop forwarding pings).
 *     - Closes the customer's WebSocket session with a DELIVERY_COMPLETE frame.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ActiveDeliveryRegistry deliveryRegistry;
    private final TrackingSessionRegistry sessionRegistry;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderEventListenerContainerFactory"
    )
    public void onOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null or malformed OrderStatusUpdatedEvent — skipping");
            return;
        }

        if (!"ORDER_STATUS_UPDATED".equals(event.getEventType())) {
            return; // Ignore OrderCreatedEvent and other event types on this topic
        }

        String newStatus = event.getPayload().getNewStatus();
        String orderId   = event.getPayload().getOrderId();
        String driverId  = event.getPayload().getDriverId();

        switch (newStatus) {
            case "OUT_FOR_DELIVERY" -> {
                if (driverId == null || driverId.isBlank()) {
                    log.warn("OUT_FOR_DELIVERY event missing driverId for orderId={}", orderId);
                    return;
                }
                // Start forwarding this driver's GPS pings to the customer's socket
                deliveryRegistry.register(driverId, orderId);
                log.info("Tracking activated: orderId={} driverId={}", orderId, driverId);
            }

            case "DELIVERED" -> {
                // Stop forwarding pings
                if (driverId != null) {
                    deliveryRegistry.deregister(driverId);
                }
                // Close the customer's WebSocket with a DELIVERY_COMPLETE frame
                sessionRegistry.close(orderId);
                log.info("Tracking closed (delivered): orderId={}", orderId);
            }

            default -> {
                // READY_FOR_PICKUP, ACCEPTED, etc. — no action needed here
                log.debug("Ignoring order event: orderId={} newStatus={}", orderId, newStatus);
            }
        }
    }
}
