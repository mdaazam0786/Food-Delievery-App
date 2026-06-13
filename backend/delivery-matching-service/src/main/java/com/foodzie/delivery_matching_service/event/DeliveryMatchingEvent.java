package com.foodzie.delivery_matching_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published to the "delivery-events" Kafka topic after the matching algorithm
 * selects the top candidate drivers.
 *
 * Consumers:
 *   - A future driver-dispatch-service reads this and sends push notifications
 *     to each candidate driver's app: "New delivery available — Accept?"
 *   - notification-service can listen to NO_DRIVERS_AVAILABLE to alert the user.
 *
 * eventType values:
 *   DELIVERY_MATCHING_INITIATED  — top N drivers found, awaiting acceptance
 *   NO_DRIVERS_AVAILABLE         — no drivers found after expanding to max radius
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryMatchingEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "DELIVERY_MATCHING_INITIATED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String source = "delivery-matching-service";

    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        private String orderId;
        private String restaurantId;
        private String cityZone;

        /**
         * Ordered list of candidate driver IDs, ranked by ETA to restaurant.
         * Empty when eventType = NO_DRIVERS_AVAILABLE.
         */
        private List<String> candidateDriverIds;

        /** Radius at which drivers were found (km). */
        private double searchRadiusKm;

        /** Reason when no drivers found, e.g. "NO_IDLE_DRIVERS_WITHIN_10KM". */
        private String failureReason;
    }
}
