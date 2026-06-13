package com.foodzie.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published to Kafka topic "payment-events" after the Razorpay webhook is verified.
 *
 * <p>Consumers:
 * <ul>
 *   <li><b>order-service</b> – transitions the order from {@code PENDING} → {@code PREPARING}
 *       and fires an {@code OrderStatusUpdatedEvent}.</li>
 *   <li><b>notification-service</b> – sends a payment receipt email / push notification.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    // ── Metadata wrapper ─────────────────────────────────────────────────────

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "PAYMENT_COMPLETED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String source = "payment-service";

    // ── Business payload ─────────────────────────────────────────────────────

    private Payload payload;

    // ── Nested types ─────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        /** Internal order ID from order-service. */
        private String orderId;

        /** Email of the customer — for notification-service. */
        private String userEmail;

        /**
         * Razorpay payment transaction ID.
         * Forwarded into {@code OrderStatusUpdatedEvent.EventMetadata.paymentTransactionId}.
         */
        private String paymentTransactionId;

        /** Amount that was charged — for cross-validation. */
        private BigDecimal amountPaid;

        /** ISO 4217 currency code. */
        private String currency;

        /** "CAPTURED" or "FAILED" — mirrors {@link com.foodzie.payment_service.data.PaymentStatus}. */
        private String status;
    }
}
