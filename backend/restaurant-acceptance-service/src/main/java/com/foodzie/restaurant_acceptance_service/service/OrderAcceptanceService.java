package com.foodzie.restaurant_acceptance_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.foodzie.restaurant_acceptance_service.dto.AcceptOrderRequest;
import com.foodzie.restaurant_acceptance_service.dto.DeclineOrderRequest;
import com.foodzie.restaurant_acceptance_service.event.OrderStatusUpdatedEvent;
import com.foodzie.restaurant_acceptance_service.event.RestaurantDecisionEvent;
import com.foodzie.restaurant_acceptance_service.model.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Core business logic for the restaurant acceptance flow.
 *
 * Responsibilities:
 *   1. On new paid order: store in Redis with TTL, push to tablet via SSE.
 *   2. On accept: remove from Redis, publish ACCEPTED event to Kafka.
 *   3. On decline: remove from Redis, publish DECLINED event to Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAcceptanceService {

    private static final String PENDING_KEY_PREFIX = "pending_order:";

    @Qualifier("pendingOrderRedisTemplate")
    private final RedisTemplate<String, PendingOrder> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SseEmitterRegistry sseRegistry;

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${app.pending-order.ttl-seconds}")
    private long ttlSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ── Inbound: new paid order ───────────────────────────────────────────────

    /**
     * Called by the Kafka listener when an ORDER_STATUS_UPDATED event arrives
     * with triggerReason = PAYMENT_SUCCESSFUL.
     *
     * Stores the order in Redis with a 5-minute TTL and pushes it to the
     * restaurant's tablet via SSE.
     */
    public void handleNewPaidOrder(OrderStatusUpdatedEvent event) {
        OrderStatusUpdatedEvent.Payload p = event.getPayload();

        // Idempotency: skip if already stored (duplicate Kafka delivery)
        String redisKey = PENDING_KEY_PREFIX + p.getOrderId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.warn("Duplicate paid order event for orderId={} — skipping", p.getOrderId());
            return;
        }

        PendingOrder pending = PendingOrder.builder()
                .orderId(p.getOrderId())
                .restaurantId(p.getRestaurantId())
                .userEmail(p.getUserEmail())
                .receivedAt(Instant.now())
                .build();

        // Store with TTL — Redis auto-expires after 5 minutes if no action taken
        redisTemplate.opsForValue().set(redisKey, pending, Duration.ofSeconds(ttlSeconds));
        log.info("Pending order stored in Redis: orderId={} restaurantId={} ttl={}s",
                p.getOrderId(), p.getRestaurantId(), ttlSeconds);

        // Push to the restaurant tablet via SSE
        try {
            String payload = objectMapper.writeValueAsString(pending);
            sseRegistry.push(p.getRestaurantId(), "new_order", payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PendingOrder for SSE push: {}", e.getMessage());
        }
    }

    // ── Accept ────────────────────────────────────────────────────────────────

    public void acceptOrder(String orderId, AcceptOrderRequest request) {
        try {
            PendingOrder pending = loadAndRemove(orderId);

            log.info("Order accepted: orderId={} restaurantId={} prepTime={}min",
                    orderId, pending.getRestaurantId(), request.getEstimatedPrepTimeMinutes());

            RestaurantDecisionEvent event = RestaurantDecisionEvent.builder()
                    .payload(RestaurantDecisionEvent.Payload.builder()
                            .orderId(orderId)
                            .userEmail(pending.getUserEmail())
                            .restaurantId(pending.getRestaurantId())
                            .previousStatus("PREPARING")
                            .newStatus("ACCEPTED")
                            .triggerReason("RESTAURANT_ACCEPTED")
                            .metadata(RestaurantDecisionEvent.DecisionMetadata.builder()
                                    .estimatedPrepTimeMinutes(request.getEstimatedPrepTimeMinutes())
                                    .build())
                            .build())
                    .build();

            publishDecision(orderId, event);
        } catch (IllegalArgumentException e) {
            log.error("❌ Failed to accept order orderId={}: {}", orderId, e.getMessage());
            throw new IllegalArgumentException(
                    "Cannot accept order: " + e.getMessage() +
                    " Please verify the order ID is correct and the payment has been processed.", e);
        }
    }

    // ── Decline ───────────────────────────────────────────────────────────────

    public void declineOrder(String orderId, DeclineOrderRequest request) {
        try {
            PendingOrder pending = loadAndRemove(orderId);

            log.info("Order declined: orderId={} restaurantId={} reason={}",
                    orderId, pending.getRestaurantId(), request.getReason());

            RestaurantDecisionEvent event = RestaurantDecisionEvent.builder()
                    .payload(RestaurantDecisionEvent.Payload.builder()
                            .orderId(orderId)
                            .userEmail(pending.getUserEmail())
                            .restaurantId(pending.getRestaurantId())
                            .previousStatus("PREPARING")
                            .newStatus("DECLINED")
                            .triggerReason("RESTAURANT_DECLINED")
                            .metadata(RestaurantDecisionEvent.DecisionMetadata.builder()
                                    .declineReason(request.getReason())
                                    .build())
                            .build())
                    .build();

            publishDecision(orderId, event);
        } catch (IllegalArgumentException e) {
            log.error("❌ Failed to decline order orderId={}: {}", orderId, e.getMessage());
            throw new IllegalArgumentException(
                    "Cannot decline order: " + e.getMessage() +
                    " Please verify the order ID is correct and the payment has been processed.", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Loads the pending order from Redis and deletes the key atomically.
     * Throws if the order is not found (already expired or acted on).
     */
    private PendingOrder loadAndRemove(String orderId) {
        String redisKey = PENDING_KEY_PREFIX + orderId;
        PendingOrder pending = redisTemplate.opsForValue().get(redisKey);
        if (pending == null) {
            throw new IllegalArgumentException(
                    "No pending order found for orderId: " + orderId
                    + ". It may have already been acted on or expired.");
        }
        redisTemplate.delete(redisKey);
        return pending;
    }

    private void publishDecision(String orderId, RestaurantDecisionEvent event) {
        kafkaTemplate.send(orderEventsTopic, orderId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish decision event for orderId={}: {}",
                                orderId, ex.getMessage());
                    } else {
                        log.info("Decision event published: orderId={} newStatus={}",
                                orderId, event.getPayload().getNewStatus());
                    }
                });
    }
}
