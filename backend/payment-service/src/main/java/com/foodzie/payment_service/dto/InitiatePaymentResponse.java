package com.foodzie.payment_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Returned to the React Native app so the Razorpay SDK modal can open.
 */
@Data
@Builder
public class InitiatePaymentResponse {
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;          // Razorpay public key — safe to expose to frontend
}
