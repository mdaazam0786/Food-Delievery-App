package com.foodzie.restaurant_acceptance_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Cached in Redis under key {@code pending_order:{orderId}} with a 5-minute TTL.
 *
 * If the restaurant doesn't act within the TTL window, Redis expires the key
 * and a keyspace notification triggers auto-cancellation via {@code RedisExpiryListener}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrder implements Serializable {

    private String orderId;
    private String restaurantId;
    private String userEmail;
    private Instant receivedAt;

    /** WAITING → ACCEPTED | DECLINED | EXPIRED */
    @Builder.Default
    private String status = "WAITING";
}
