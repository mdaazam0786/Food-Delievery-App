package com.foodzie.payment_service.service.implementation;

import com.foodzie.payment_service.data.Payment;
import com.foodzie.payment_service.data.PaymentStatus;
import com.foodzie.payment_service.dto.InitiatePaymentRequest;
import com.foodzie.payment_service.dto.InitiatePaymentResponse;
import com.foodzie.payment_service.event.PaymentCompletedEvent;
import com.foodzie.payment_service.event.PaymentEventPublisher;
import com.foodzie.payment_service.repository.PaymentRepository;
import com.foodzie.payment_service.service.PaymentService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final PaymentEventPublisher eventPublisher;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    // ── Retry Configuration for Kafka Event Delay ──────────────────────────────
    private static final int PAYMENT_LOOKUP_RETRY_ATTEMPTS = 3;
    private static final long PAYMENT_LOOKUP_RETRY_DELAY_MS = 300;

    // ── Concurrency Control: Order ID-based locks ───────────────────────────────
    // Protects against Razorpay sending duplicate webhooks in rapid succession.
    // Each orderId gets its own lock to prevent race conditions during status updates.
    private final ConcurrentHashMap<String, Object> orderLocks = new ConcurrentHashMap<>();

    // ── /api/payments/initiate ────────────────────────────────────────────────

    @Override
    @Transactional
    public InitiatePaymentResponse initiatePayment(String userEmail, InitiatePaymentRequest request) {
        // ────────────────────────────────────────────────────────────────────────
        // STEP 1: Retry loop to wait for Kafka consumer to create payment record
        // ────────────────────────────────────────────────────────────────────────
        Payment payment = lookupPaymentWithRetry(request.getOrderId());

        // ────────────────────────────────────────────────────────────────────────
        // STEP 2: Ownership check — the JWT subject must match the payment's user
        // ────────────────────────────────────────────────────────────────────────
        if (!payment.getUserEmail().equals(userEmail)) {
            throw new SecurityException("Payment does not belong to this user");
        }

        // ────────────────────────────────────────────────────────────────────────
        // STEP 3: Idempotency — if Razorpay order already created, return it
        // ────────────────────────────────────────────────────────────────────────
        if (payment.getRazorpayOrderId() != null) {
            log.info("Returning existing Razorpay order for orderId={}", request.getOrderId());
            return buildInitiateResponse(payment);
        }

        try {
            // ────────────────────────────────────────────────────────────────────
            // STEP 4: Create Razorpay order
            // ────────────────────────────────────────────────────────────────────
            // Razorpay expects amount in the smallest currency unit (paise for INR).
            int amountInPaise = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", payment.getCurrency());
            // receipt is a short reference string visible in the Razorpay dashboard.
            orderRequest.put("receipt", "foodzie_" + payment.getOrderId());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            payment.setRazorpayOrderId(razorpayOrderId);
            paymentRepository.save(payment);

            log.info("Razorpay order created: razorpayOrderId={} for orderId={}",
                    razorpayOrderId, request.getOrderId());

            return buildInitiateResponse(payment);

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for orderId={}: {}",
                    request.getOrderId(), e.getMessage());
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    /**
     * Lookup payment record with retry loop to handle Kafka event delay.
     * 
     * The race condition: Frontend calls /initiate immediately after order creation,
     * but Kafka consumer may not have created the payment record yet due to:
     * - Network latency
     * - Kafka broker processing delay
     * - Consumer group assignment delay
     * 
     * Solution: Poll up to 3 times with 300ms delay between attempts.
     * If still not found, fetch order details from order-service and create
     * the payment record on-the-fly (fallback mechanism).
     * 
     * @param orderId the order ID to look up
     * @return the Payment record (either from Kafka-created or fallback-created)
     * @throws IllegalArgumentException if lookup fails after all retries and fallback attempts
     */
    private Payment lookupPaymentWithRetry(String orderId) {
        for (int attempt = 1; attempt <= PAYMENT_LOOKUP_RETRY_ATTEMPTS; attempt++) {
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
            
            if (paymentOpt.isPresent()) {
                log.info("Payment record found for orderId={} on attempt {}", orderId, attempt);
                return paymentOpt.get();
            }
            
            if (attempt < PAYMENT_LOOKUP_RETRY_ATTEMPTS) {
                log.debug("Payment record not found for orderId={} on attempt {} — retrying in {}ms",
                        orderId, attempt, PAYMENT_LOOKUP_RETRY_DELAY_MS);
                try {
                    Thread.sleep(PAYMENT_LOOKUP_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during payment lookup retry", e);
                }
            }
        }

        // ────────────────────────────────────────────────────────────────────────
        // FALLBACK: Payment record not found after retries
        // ────────────────────────────────────────────────────────────────────────
        log.warn("Payment record not found for orderId={} after {} retries — " +
                "attempting fallback payment creation", orderId, PAYMENT_LOOKUP_RETRY_ATTEMPTS);
        
        return createPaymentRecordFallback(orderId);
    }

    /**
     * Fallback mechanism: Create payment record dynamically when Kafka event hasn't arrived.
     * 
     * This method fetches the order details from the order-service repository 
     * (via direct access or will be extended to use Feign Client if needed)
     * and creates an initial payment record with the order's financial data.
     * 
     * This ensures the frontend checkout doesn't fail even if Kafka event delay is excessive.
     * Idempotency is maintained by checking if record was just created by Kafka consumer.
     * 
     * @param orderId the order ID
     * @return the Payment record (newly created)
     * @throws IllegalArgumentException if order not found or cannot be fetched
     */
    private Payment createPaymentRecordFallback(String orderId) {
        // One final check before creating — avoid race condition where Kafka just created it
        Optional<Payment> finalCheckOpt = paymentRepository.findByOrderId(orderId);
        if (finalCheckOpt.isPresent()) {
            log.info("Payment record appeared for orderId={} during fallback check", orderId);
            return finalCheckOpt.get();
        }

        // TODO: Implement Order Service Feign Client call to fetch order details
        // For now, log the issue and provide guidance.
        // 
        // The fallback should:
        // 1. Call OrderServiceClient.getOrderById(orderId) to fetch Order
        // 2. Extract userEmail, totalAmount, currency from Order
        // 3. Create Payment record with those details
        // 4. Save and return
        //
        // Example (once Feign client is added):
        // Order order = orderServiceClient.getOrderById(orderId);
        // if (order == null) {
        //     throw new IllegalArgumentException("Order not found: " + orderId);
        // }
        // Payment payment = Payment.builder()
        //         .orderId(orderId)
        //         .userEmail(order.getUserEmail())
        //         .amount(order.getTotalAmount())
        //         .currency(order.getCurrency())
        //         .build();
        // Payment saved = paymentRepository.save(payment);
        // log.info("Payment record created via fallback for orderId={}", orderId);
        // return saved;

        // ────────────────────────────────────────────────────────────────────────
        // TEMPORARY: Throw error with helpful message for setup guidance
        // ────────────────────────────────────────────────────────────────────────
        log.error("Payment record not found and fallback creation not yet implemented for orderId={}. " +
                "Please ensure: (1) Order Service is accessible, (2) Kafka is configured correctly, " +
                "(3) OrderCreatedEventListener is running and processing events", orderId);
        
        throw new IllegalArgumentException(
                "No payment record found for orderId: " + orderId +
                ". The Kafka event may be delayed. Please retry in a moment. " +
                "If this persists, contact support with order ID: " + orderId);
    }

    // ── /api/payments/webhook ─────────────────────────────────────────────────

    @Override
    @Transactional
    public void handleWebhook(String rawPayload, String razorpaySignature) {
        // ── Step 1: Cryptographic signature verification ──────────────────────
        // This is the most critical security gate. We reject anything that doesn't
        // match the HMAC-SHA256 of (rawPayload + webhookSecret).
        try {
            boolean isValid = Utils.verifyWebhookSignature(rawPayload, razorpaySignature, webhookSecret);
            if (!isValid) {
                log.warn("Razorpay webhook rejected — signature mismatch");
                throw new SecurityException("Invalid webhook signature");
            }
        } catch (RazorpayException e) {
            throw new SecurityException("Webhook signature verification failed: " + e.getMessage());
        }

        // ── Step 2: Parse the event ───────────────────────────────────────────
        JSONObject event = new JSONObject(rawPayload);
        String eventType = event.getString("event");
        log.info("Razorpay webhook verified: event={}", eventType);

        // We only care about payment.captured and payment.failed.
        if (!"payment.captured".equals(eventType) && !"payment.failed".equals(eventType)) {
            log.debug("Ignoring unhandled Razorpay event type: {}", eventType);
            return;
        }

        JSONObject paymentEntity = event
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayOrderId   = paymentEntity.getString("order_id");
        String razorpayPaymentId = paymentEntity.getString("id");

        // ── Step 3: Load the payment record ──────────────────────────────────
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment record found for razorpayOrderId: " + razorpayOrderId));

        String orderId = payment.getOrderId();

        // ── Step 4: Acquire lock for this orderId ────────────────────────────
        // This synchronization block ensures that rapid-fire duplicate webhooks
        // from Razorpay don't cause race conditions where multiple threads read
        // the same CREATED status simultaneously and attempt to update it.
        Object lock = orderLocks.computeIfAbsent(orderId, k -> new Object());

        synchronized (lock) {
            // ── Step 5: Re-fetch and re-check status inside synchronized block ──
            // A concurrent webhook may have already updated the status.
            // This second check prevents duplicate processing.
            Payment freshPayment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Payment record disappeared: " + razorpayOrderId));

            if (freshPayment.getStatus() != PaymentStatus.CREATED) {
                log.warn("⚠️  DUPLICATE WEBHOOK DETECTED — orderId={} already in status: {} | " +
                        "Razorpay Payment ID: {} | Event Type: {} | " +
                        "This is likely a concurrent duplicate from Razorpay — safely ignoring.",
                        orderId, freshPayment.getStatus().name(), razorpayPaymentId, eventType);
                return;
            }

            // ── Step 6: Update payment status (Critical Section) ──────────────
            freshPayment.setRazorpayPaymentId(razorpayPaymentId);
            freshPayment.setRazorpaySignature(razorpaySignature); // stored for audit trail

            if ("payment.captured".equals(eventType)) {
                freshPayment.setStatus(PaymentStatus.CAPTURED);
                paymentRepository.save(freshPayment);

                // ── Trace Logging: Verify database persistence ──────────────────
                String verificationStatus = paymentRepository.findById(freshPayment.getId())
                        .map(p -> p.getStatus().name())
                        .orElse("NOT FOUND");
                log.info("💾 MongoDB save execution fired. Fetching immediately to verify status: {}", verificationStatus);

                log.info("Payment CAPTURED: orderId={} razorpayPaymentId={}",
                        orderId, razorpayPaymentId);

                // ── Step 7: Broadcast PaymentCompletedEvent ───────────────────────
                // order-service listens → PENDING → PREPARING
                // notification-service listens → sends receipt to user
                eventPublisher.publishPaymentCompleted(PaymentCompletedEvent.builder()
                        .payload(PaymentCompletedEvent.Payload.builder()
                                .orderId(orderId)
                                .userEmail(freshPayment.getUserEmail())
                                .paymentTransactionId(razorpayPaymentId)
                                .amountPaid(freshPayment.getAmount())
                                .currency(freshPayment.getCurrency())
                                .status(PaymentStatus.CAPTURED.name())
                                .build())
                        .build());

            } else {
                // payment.failed
                freshPayment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(freshPayment);

                // ── Trace Logging: Verify database persistence ──────────────────
                String verificationStatus = paymentRepository.findById(freshPayment.getId())
                        .map(p -> p.getStatus().name())
                        .orElse("NOT FOUND");
                log.info("💾 MongoDB save execution fired for FAILED payment. Fetching immediately to verify status: {}", verificationStatus);

                log.warn("Payment FAILED: orderId={} razorpayPaymentId={}",
                        orderId, razorpayPaymentId);

                eventPublisher.publishPaymentCompleted(PaymentCompletedEvent.builder()
                        .payload(PaymentCompletedEvent.Payload.builder()
                                .orderId(orderId)
                                .userEmail(freshPayment.getUserEmail())
                                .paymentTransactionId(razorpayPaymentId)
                                .amountPaid(freshPayment.getAmount())
                                .currency(freshPayment.getCurrency())
                                .status(PaymentStatus.FAILED.name())
                                .build())
                        .build());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InitiatePaymentResponse buildInitiateResponse(Payment payment) {
        return InitiatePaymentResponse.builder()
                .razorpayOrderId(payment.getRazorpayOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .keyId(razorpayKeyId)
                .build();
    }
}
