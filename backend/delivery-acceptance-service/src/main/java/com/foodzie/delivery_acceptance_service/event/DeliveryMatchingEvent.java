package com.foodzie.delivery_acceptance_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mirror of delivery-matching-service's DeliveryMatchingEvent.
 * Consumed from the "delivery-events" Kafka topic.
 *
 * This service acts on:
 *   DELIVERY_MATCHING_INITIATED — broadcast offer to candidateDriverIds
 *   NO_DRIVERS_AVAILABLE        — ignored here (handled by notification-service)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryMatchingEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String source;
    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String orderId;
        private String restaurantId;
        private String cityZone;
        private List<String> candidateDriverIds;
        private double searchRadiusKm;
        private String failureReason;
    }
}
