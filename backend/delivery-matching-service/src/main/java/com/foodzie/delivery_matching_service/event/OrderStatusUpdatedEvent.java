package com.foodzie.delivery_matching_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Mirror of order-service's OrderStatusUpdatedEvent.
 * Consumed from the "order-events" Kafka topic.
 *
 * This service only acts when payload.newStatus = "ACCEPTED"
 * (restaurant has confirmed the order — time to find a driver).
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
        private String previousStatus;
        private String newStatus;
        private String triggerReason;
        private EventMetadata metadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMetadata {
        private String paymentTransactionId;
        private Integer estimatedPrepTimeMinutes;
    }
}
