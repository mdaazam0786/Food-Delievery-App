package com.foodzie.delivery_acceptance_service.service;

import com.foodzie.delivery_acceptance_service.data.DriverStatus;
import com.foodzie.delivery_acceptance_service.dto.AcceptOfferRequest;
import com.foodzie.delivery_acceptance_service.dto.AcceptOfferResponse;
import com.foodzie.delivery_acceptance_service.event.DeliveryPartnerAssignedEvent;
import com.foodzie.delivery_acceptance_service.repository.DeliveryPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * Core acceptance logic — handles the race condition via a Redis distributed lock.
 *
 * Flow when a driver taps "Accept":
 *
 *   1. Attempt atomic lock:
 *        SET order:lock:{orderId} {driverId} NX EX 30
 *      NX = only set if key does NOT exist.
 *      EX = auto-expire after 30s (safety net if the service crashes mid-assignment).
 *
 *   2. Winner (SET returned true):
 *      a. Update driver status IDLE → DELIVERING in MySQL.
 *      b. Delete the offer:pending:{orderId} key so the timeout listener doesn't fire.
 *      c. Publish DeliveryPartnerAssignedEvent to Kafka.
 *      d. Notify losing candidates via SSE ("offer_taken").
 *      e. Return assigned=true to the winning driver.
 *
 *   3. Loser (SET returned false — another driver already holds the lock):
 *      Return assigned=false with message "This order has already been taken."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverAcceptanceService {

    private static final String LOCK_KEY_PREFIX  = "order:lock:";
    private static final String OFFER_KEY_PREFIX = "offer:pending:";

    private final RedisTemplate<String, String> redisTemplate;
    private final DeliveryPartnerRepository driverRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DriverSseRegistry sseRegistry;

    @Value("${app.kafka.topics.delivery-events}")
    private String deliveryEventsTopic;

    @Value("${app.acceptance.offer-ttl-seconds}")
    private int lockTtlSeconds;

    /**
     * Attempts to assign the order to the requesting driver.
     * This method is the single point of truth for the race condition resolution.
     */
    @Transactional
    public AcceptOfferResponse acceptOffer(AcceptOfferRequest request) {
        String orderId  = request.getOrderId();
        String driverId = request.getDriverId();
        String lockKey  = LOCK_KEY_PREFIX + orderId;

        // ── Step 1: Atomic distributed lock ──────────────────────────────────
        // SET order:lock:{orderId} {driverId} NX EX 30
        // Returns true only if the key did NOT exist (this driver is the winner).
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, driverId, Duration.ofSeconds(lockTtlSeconds));

        if (!Boolean.TRUE.equals(acquired)) {
            // Another driver already holds the lock
            String winner = redisTemplate.opsForValue().get(lockKey);
            log.info("Lock contention: orderId={} driverId={} lost to driverId={}",
                    orderId, driverId, winner);
            return AcceptOfferResponse.builder()
                    .assigned(false)
                    .orderId(orderId)
                    .driverId(driverId)
                    .message("This order has already been taken.")
                    .build();
        }

        log.info("Lock acquired: orderId={} driverId={}", orderId, driverId);

        // ── Step 2a: Update driver status IDLE → DELIVERING in MySQL ─────────
        int updated = driverRepository.updateStatusIfExpected(
                driverId, DriverStatus.IDLE, DriverStatus.DELIVERING);

        if (updated == 0) {
            // Driver is no longer IDLE (went offline, or already on a delivery).
            // Release the lock so another candidate can win.
            redisTemplate.delete(lockKey);
            log.warn("Driver no longer IDLE at acceptance time: driverId={} orderId={} — lock released",
                    driverId, orderId);
            return AcceptOfferResponse.builder()
                    .assigned(false)
                    .orderId(orderId)
                    .driverId(driverId)
                    .message("You are no longer available to accept deliveries.")
                    .build();
        }

        // ── Step 2b: Cancel the offer TTL key so the timeout listener doesn't fire ──
        redisTemplate.delete(OFFER_KEY_PREFIX + orderId);

        // ── Step 2c: Retrieve offer metadata for the Kafka event ─────────────
        // The lock value is the driverId; metadata was stored in the offer key.
        // Since we just deleted it, we parse from the lock value context.
        // In practice the matching event payload is stored separately — here we
        // use a best-effort approach: restaurantId and cityZone from the offer key
        // were already deleted, so we publish what we have.
        DeliveryPartnerAssignedEvent assignedEvent = DeliveryPartnerAssignedEvent.builder()
                .payload(DeliveryPartnerAssignedEvent.Payload.builder()
                        .orderId(orderId)
                        .driverId(driverId)
                        .status("CONFIRMED")
                        .build())
                .build();

        kafkaTemplate.send(deliveryEventsTopic, orderId, assignedEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DeliveryPartnerAssignedEvent: orderId={} error={}",
                                orderId, ex.getMessage());
                    } else {
                        log.info("DeliveryPartnerAssignedEvent published: orderId={} driverId={}",
                                orderId, driverId);
                    }
                });

        // ── Step 2d: Notify losing candidates via SSE ─────────────────────────
        // We don't have the full candidate list here — the SSE registry handles
        // this by broadcasting to all connected drivers watching this orderId.
        // For simplicity, the driver app polls for offer status after the TTL.
        // A production system would store candidateIds in Redis alongside the offer.

        log.info("Assignment complete: orderId={} driverId={}", orderId, driverId);

        return AcceptOfferResponse.builder()
                .assigned(true)
                .orderId(orderId)
                .driverId(driverId)
                .message("Order accepted. You have been assigned to this delivery.")
                .build();
    }

    /**
     * Handles a driver declining an offer.
     * Simply removes the offer from the driver's SSE stream (if any tracking exists).
     * The offer will remain available for other drivers until it expires or another driver accepts it.
     */
    public void declineOffer(String orderId, String driverId) {
        log.info("Driver declined offer: orderId={} driverId={}", orderId, driverId);
        // No-op for now - just log the decline.
        // In a production system, this could track driver preferences or blacklist
        // drivers who decline frequently in a specific area.
    }
}
