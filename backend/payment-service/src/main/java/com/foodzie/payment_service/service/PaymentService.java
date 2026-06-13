package com.foodzie.payment_service.service;

import com.foodzie.payment_service.dto.InitiatePaymentRequest;
import com.foodzie.payment_service.dto.InitiatePaymentResponse;

public interface PaymentService {

    /** Called by frontend — creates a Razorpay order and returns the checkout details. */
    InitiatePaymentResponse initiatePayment(String userEmail, InitiatePaymentRequest request);

    /** Called internally by the webhook handler after signature validation. */
    void handleWebhook(String payload, String razorpaySignature);
}
