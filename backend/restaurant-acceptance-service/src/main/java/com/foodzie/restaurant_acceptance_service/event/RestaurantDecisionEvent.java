package com.foodzie.restaurant_acceptance_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to "order-events" when a restaurant accepts or declines an order,
 * or when the 5-minute TTL expires with no action.
 *
 * Consumers:
 *   order-service        — updates master order status to ACCEPTED / DECLINED
 *   notification-service — pushes result to the customer's phone
 *   delivery-matching-service — wakes up to find a driver when newStatus = ACCEPTED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDecisionEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "ORDER_STATUS_UPDATED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String source = "restaurant-acceptance-service";

    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String orderId;
        private String userEmail;
        private String restaurantId;
        private String previousStatus;  // always "PREPARING"
        private String newStatus;       // "ACCEPTED" | "DECLINED"
        private String triggerReason;   // "RESTAURANT_ACCEPTED" | "RESTAURANT_DECLINED" | "TIMEOUT"
        private DecisionMetadata metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionMetadata {
        private Integer estimatedPrepTimeMinutes;  // set on accept
        private String declineReason;              // set on decline / timeout
    }
}
