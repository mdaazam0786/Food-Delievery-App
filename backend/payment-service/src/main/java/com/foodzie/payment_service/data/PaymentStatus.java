package com.foodzie.payment_service.data;

public enum PaymentStatus {
    CREATED,    // Razorpay order created, awaiting user payment
    CAPTURED,   // Payment confirmed via Razorpay webhook
    FAILED,     // Payment failed or was declined
    REFUNDED    // Payment was refunded
}
