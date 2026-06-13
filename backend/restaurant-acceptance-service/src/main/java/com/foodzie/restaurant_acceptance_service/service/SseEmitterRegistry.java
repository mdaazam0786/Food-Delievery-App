package com.foodzie.restaurant_acceptance_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of active SSE connections, keyed by restaurantId.
 *
 * When a restaurant tablet opens the stream endpoint, a new SseEmitter is registered here.
 * When a new order arrives for that restaurant, the Kafka listener calls push() to send
 * the JSON payload down the open pipe, making the tablet ring immediately.
 *
 * Thread-safety: ConcurrentHashMap handles concurrent reads/writes from the Kafka consumer
 * thread and the HTTP request threads.
 *
 * Limitation: single-node registry. In a multi-instance deployment you'd replace this with
 * Redis Pub/Sub fan-out so any node can push to any tablet.
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    @Value("${app.sse.timeout-ms}")
    private long sseTimeoutMs;

    /** One active emitter per restaurant. A new connection replaces the old one. */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Creates and registers a new SSE emitter for the given restaurant.
     * If the restaurant already has an open connection, it is completed first.
     */
    public SseEmitter register(String restaurantId) {
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);

        // Clean up on completion, timeout, or error
        emitter.onCompletion(() -> {
            emitters.remove(restaurantId);
            log.info("SSE connection closed for restaurantId={}", restaurantId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(restaurantId);
            log.info("SSE connection timed out for restaurantId={}", restaurantId);
        });
        emitter.onError(e -> {
            emitters.remove(restaurantId);
            log.warn("SSE connection error for restaurantId={}: {}", restaurantId, e.getMessage());
        });

        // Replace any existing connection (tablet reconnected)
        SseEmitter existing = emitters.put(restaurantId, emitter);
        if (existing != null) {
            existing.complete();
        }

        log.info("SSE emitter registered for restaurantId={}", restaurantId);

        // Send an initial "connected" event so the tablet knows the stream is live
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"restaurantId\":\"" + restaurantId + "\",\"status\":\"CONNECTED\"}"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event to restaurantId={}", restaurantId);
        }

        return emitter;
    }

    /**
     * Pushes a JSON payload to the tablet connected for the given restaurant.
     * If no tablet is connected, the push is silently dropped — the order is still
     * persisted in Redis and will be visible when the tablet reconnects.
     */
    public void push(String restaurantId, String eventName, String payload) {
        SseEmitter emitter = emitters.get(restaurantId);
        if (emitter == null) {
            log.warn("No active SSE connection for restaurantId={} — push dropped", restaurantId);
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
            log.debug("SSE push sent to restaurantId={} event={}", restaurantId, eventName);
        } catch (IOException e) {
            log.warn("SSE push failed for restaurantId={}: {}", restaurantId, e.getMessage());
            emitters.remove(restaurantId);
            emitter.completeWithError(e);
        }
    }
}
