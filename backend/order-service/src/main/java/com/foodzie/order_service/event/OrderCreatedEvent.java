package com.foodzie.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published to Kafka topic "order-events" when a new order is successfully saved.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>payment-service</b> – reads {@code payload.financials} to create a Razorpay Payment Intent.</li>
 *   <li><b>notification-service</b> (optional) – reads {@code payload.userEmail} to send an
 *       "Order received, waiting for payment" email.</li>
 * </ul>
 *
 * <p>The top-level metadata wrapper ({@code eventId}, {@code eventType}, {@code timestamp},
 * {@code source}) follows standard event-sourcing practice so any failed event can be
 * uniquely identified and replayed from logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    // ── Metadata wrapper ─────────────────────────────────────────────────────

    /** Globally unique event identifier. Generated at publish time. */
    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "ORDER_CREATED";

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

        /** MongoDB ObjectId of the saved order. */
        private String orderId;

        /** Email of the customer who placed the order (from JWT subject). */
        private String userEmail;

        /** References the restaurant in the catalog service. */
        private String restaurantId;

        /** References the delivery address saved in user-service. */
        private Long deliveryAddressId;

        /** Financial summary — the only thing payment-service needs. */
        private Financials financials;

        /**
         * Full item list — price snapshots at the time of purchase.
         * payment-service ignores this; notification-service may use it
         * to render an order summary email.
         */
        private List<OrderItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Financials {
        private BigDecimal foodSubtotal;
        private BigDecimal deliveryFee;
        private BigDecimal gstAmount;
        private BigDecimal totalAmount;

        /** ISO 4217 currency code, e.g. "INR". */
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String menuItemId;
        private String itemName;
        private Integer quantity;

        /** Price per unit at the time of purchase — never the live catalog price. */
        private BigDecimal priceAtPurchase;
    }
}
