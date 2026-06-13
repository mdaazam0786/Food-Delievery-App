package com.foodzie.delivery_acceptance_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to "delivery-events" when the 30-second offer window expires
 * with no driver accepting.
 *
 * Consumers:
 *   delivery-matching-service — widens the search radius and re-runs matching
 *   notification-service      — optionally informs the user of a delay
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferExpiredEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "DELIVERY_OFFER_EXPIRED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String source = "delivery-acceptance-service";

    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String orderId;
        private String restaurantId;
        private String cityZone;

        /** The radius (km) that was used in the failed broadcast round. */
        private double previousRadiusKm;

        private String reason;
    }
}
