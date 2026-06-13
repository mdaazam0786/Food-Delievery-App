package com.foodzie.order_fulfillment_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published to "order-events" on every fulfillment state transition.
 *
 * Consumers:
 *   notification-service — sends push notifications to the relevant actor
 *   payment-service      — on newStatus=DELIVERED, settles funds with the restaurant
 *   websocket-manager    — on newStatus=DELIVERED, closes the live tracking connection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "ORDER_STATUS_UPDATED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String source = "order-fulfillment-service";

    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String orderId;
        private String userEmail;
        private String restaurantId;
        private String driverId;
        private String previousStatus;
        private String newStatus;

        /**
         * Machine-readable reason for the transition.
         * CHEF_MARKED_READY | DRIVER_PICKED_UP | DRIVER_CONFIRMED_DELIVERY
         */
        private String triggerReason;

        private EventMetadata metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMetadata {
        /**
         * Populated on DELIVERED — payment-service uses this to settle funds.
         * Carries the order total so payment-service doesn't need a DB lookup.
         */
        private BigDecimal orderTotal;
        private String currency;
    }
}
