package com.foodzie.payment_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
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
        try {
            log.info("Payment initiation request received");
            
            // Validate input
            if (jwt == null) {
                log.warn("JWT token is null");
                throw new UnauthorizedException("Authentication required");
            }
            if (request == null) {
                log.warn("Payment request is null");
                throw new BadRequestException("Payment request cannot be empty");
            }
            
            String userEmail = jwt.getSubject();
            
            if (userEmail == null || userEmail.isBlank()) {
                log.warn("User email is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: email claim required");
            }
            
            InitiatePaymentResponse response = paymentService.initiatePayment(userEmail, request);
            log.info("Payment initiated successfully");
            return ResponseEntity.ok(ApiResponse.ok("Payment initiated", response));
            
        } catch (BadRequestException | UnauthorizedException e) {
            log.warn("Bad request during payment initiation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error initiating payment", e);
            throw new InternalServerException("Failed to initiate payment", e);
        }
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
        try {
            log.debug("Webhook received from Razorpay");
            
            // Validate input
            if (payload == null || payload.isBlank()) {
                log.warn("Webhook payload is null or blank");
                throw new BadRequestException("Webhook payload is required");
            }
            if (signature == null || signature.isBlank()) {
                log.warn("Razorpay signature is null or blank");
                throw new BadRequestException("Razorpay signature is required");
            }
            
            paymentService.handleWebhook(payload, signature);
            log.debug("Webhook processed successfully");
            return ResponseEntity.ok().build();
            
        } catch (BadRequestException e) {
            log.warn("Bad request in webhook: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing webhook", e);
            throw new InternalServerException("Failed to process webhook", e);
        }
    }
}
