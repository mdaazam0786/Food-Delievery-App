package com.foodzie.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to Kafka topic "order-events" whenever an order's lifecycle status changes.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>restaurant-acceptance-service</b> – when {@code newStatus = PREPARING}, pings the
 *       restaurant tablet with the new paid order.</li>
 *   <li><b>notification-service</b> – when {@code newStatus = PREPARING}, sends a push
 *       notification to the customer: "Payment successful! Restaurant is preparing your order."</li>
 * </ul>
 *
 * <p>{@code previousStatus} + {@code triggerReason} enable full state-machine reconstruction
 * for debugging and analytics without querying the order-service database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {

    // ── Metadata wrapper ─────────────────────────────────────────────────────

    /** Globally unique event identifier. Generated at publish time. */
    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "ORDER_STATUS_UPDATED";

    /** UTC instant when the event was created. */
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String source = "order-service";

    // ── Business payload ─────────────────────────────────────────────────────

    private Payload payload;

    // ── Nested types ─────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        /** MongoDB ObjectId of the order. */
        private String orderId;

        /** Email of the customer (from the saved order). */
        private String userEmail;

        /** References the restaurant — needed by restaurant-acceptance-service. */
        private String restaurantId;

        /** The status the order was in before this transition. */
        private String previousStatus;

        /** The status the order has just moved to. */
        private String newStatus;

        /**
         * Machine-readable reason for the transition.
         * Examples: PAYMENT_SUCCESSFUL, ADMIN_OVERRIDE, RESTAURANT_CONFIRMED.
         */
        private String triggerReason;

        /**
         * Optional key-value bag for trigger-specific data.
         * e.g. when reason = PAYMENT_SUCCESSFUL, contains the Razorpay transaction ID.
         */
        private EventMetadata metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMetadata {

        /** Razorpay payment ID — populated when triggerReason = PAYMENT_SUCCESSFUL. */
        private String paymentTransactionId;

        /** Estimated prep time in minutes — populated when triggerReason = RESTAURANT_ACCEPTED. */
        private Integer estimatedPrepTimeMinutes;

        /** Decline reason — populated when triggerReason = RESTAURANT_DECLINED or TIMEOUT. */
        private String declineReason;
    }
}
