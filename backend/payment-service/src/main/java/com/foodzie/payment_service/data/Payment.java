package com.foodzie.payment_service.data;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * High-integrity financial record stored in MySQL (ACID-compliant).
 * One row per order — tracks the full lifecycle of a payment from
 * CREATED → CAPTURED (or FAILED / REFUNDED).
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_order_id",        columnList = "order_id",         unique = true),
        @Index(name = "idx_payments_razorpay_order_id", columnList = "razorpay_order_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * MongoDB ObjectId from order-service — stored as VARCHAR.
     * Unique: one payment record per order.
     */
    @Column(name = "order_id", nullable = false, unique = true, length = 36)
    private String orderId;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    /** Transaction amount in the base unit (e.g. 850.50 INR). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** ISO 4217 currency code. */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    /**
     * Order ID returned by Razorpay after calling /v1/orders.
     * Sent to the React Native SDK to open the checkout modal.
     * Nullable until /initiate is called.
     */
    @Column(name = "razorpay_order_id", unique = true, length = 64)
    private String razorpayOrderId;

    /**
     * Payment transaction ID returned by Razorpay after the user pays.
     * Populated from the webhook payload.
     */
    @Column(name = "razorpay_payment_id", length = 64)
    private String razorpayPaymentId;

    /**
     * HMAC-SHA256 signature sent by Razorpay in the webhook.
     * Stored for audit purposes after successful verification.
     */
    @Column(name = "razorpay_signature", columnDefinition = "TEXT")
    private String razorpaySignature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
