package com.foodzie.websocket_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub backplane for cross-pod WebSocket delivery.
 *
 * The scaling problem:
 *   - 100,000 active orders → multiple websocket-manager pods
 *   - User A connects to Pod 1. Driver's Kafka ping is consumed by Pod 2.
 *   - Pod 2 doesn't hold User A's WebSocket session → map stops moving.
 *
 * The solution:
 *   When any pod consumes a driver ping from Kafka, it:
 *     1. Checks its local session registry first (fast path — no Redis needed).
 *     2. If not found locally, PUBLISHes to Redis channel: ws:location:{orderId}
 *     3. All pods are subscribed to that channel.
 *     4. The pod that holds the session receives the message and pushes to the socket.
 *
 * Redis channel naming: ws:location:{orderId}
 * Message format: raw JSON string — same TrackingPayload pushed to the WebSocket.
 *
 * This is a fan-out broadcast — all pods receive the message, but only the one
 * holding the session will actually push it. The others discard it silently.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLocationBackplane {

    private static final String CHANNEL_PREFIX = "ws:location:";

    private final RedisTemplate<String, String> redisTemplate;
    private final TrackingSessionRegistry sessionRegistry;

    /**
     * Publishes a location payload to the Redis backplane channel for this order.
     * Called by the Kafka consumer when the session is NOT on this pod.
     */
    public void publish(String orderId, String jsonPayload) {
        String channel = CHANNEL_PREFIX + orderId;
        redisTemplate.convertAndSend(channel, jsonPayload);
        log.debug("Backplane publish: channel={}", channel);
    }

    /**
     * Called by the Redis message listener when a backplane message arrives on this pod.
     * Attempts to push to the local session registry.
     */
    public void onBackplaneMessage(String orderId, String jsonPayload) {
        boolean pushed = sessionRegistry.push(orderId, jsonPayload);
        if (pushed) {
            log.debug("Backplane delivery: orderId={} pushed to local session", orderId);
        }
        // If not pushed, this pod doesn't hold the session either — silently discard.
        // This is expected: all pods receive the broadcast, only one has the session.
    }

    public String channelFor(String orderId) {
        return CHANNEL_PREFIX + orderId;
    }
}
