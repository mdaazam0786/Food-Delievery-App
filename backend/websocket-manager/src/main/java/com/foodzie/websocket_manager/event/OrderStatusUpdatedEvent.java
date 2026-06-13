package com.foodzie.websocket_manager.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Mirror of order-fulfillment-service's OrderStatusUpdatedEvent.
 * Consumed from the "order-events" Kafka topic.
 *
 * This service acts on:
 *   OUT_FOR_DELIVERY — start forwarding driver GPS pings to the customer's socket
 *   DELIVERED        — close the WebSocket session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String orderId;
        private String userEmail;
        private String restaurantId;
        private String driverId;
        private String previousStatus;
        private String newStatus;
        private String triggerReason;
    }
}
