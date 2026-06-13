package com.foodzie.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inbound Kafka event consumed from the "payment-events" topic.
 * Published by payment-service after Razorpay confirms a successful payment webhook.
 *
 * <p>The order-service listens for this event to transition the order from
 * {@code PENDING} → {@code PREPARING} and then broadcast an
 * {@link OrderStatusUpdatedEvent} to all downstream consumers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    /** Mirrors the metadata wrapper published by payment-service. */
    private String eventId;
    private String eventType;   // "PAYMENT_COMPLETED"
    private String source;      // "payment-service"

    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        /** The order this payment belongs to. */
        private String orderId;

        /** Razorpay payment ID — forwarded into the OrderStatusUpdatedEvent metadata. */
        private String paymentTransactionId;

        /** Amount that was charged — for cross-validation if needed. */
        private BigDecimal amountPaid;

        /** ISO 4217 currency code. */
        private String currency;
    }
}
