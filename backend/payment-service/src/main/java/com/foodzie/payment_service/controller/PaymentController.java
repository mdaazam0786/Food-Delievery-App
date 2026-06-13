package com.foodzie.payment_service.controller;

import com.foodzie.payment_service.dto.ApiResponse;
import com.foodzie.payment_service.dto.InitiatePaymentRequest;
import com.foodzie.payment_service.dto.InitiatePaymentResponse;
import com.foodzie.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/initiate
     * Called by the React Native app after order is placed.
     * Returns Razorpay order details so the SDK checkout modal can open.
     */
    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InitiatePaymentRequest request) {
        String userEmail = jwt.getSubject();
        InitiatePaymentResponse response = paymentService.initiatePayment(userEmail, request);
        return ResponseEntity.ok(ApiResponse.ok("Payment initiated", response));
    }

    /**
     * POST /api/payments/webhook
     * Called by Razorpay — PUBLIC endpoint (no JWT).
     * Validates the HMAC signature before processing.
     * Raw body is required for signature verification — do NOT use @RequestBody DTO here.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
