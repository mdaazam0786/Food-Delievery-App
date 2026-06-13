package com.foodzie.delivery_acceptance_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodzie.delivery_acceptance_service.dto.OfferPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of active SSE connections from driver apps.
 *
 * Each driver's app opens a persistent SSE connection on startup.
 * When an offer arrives, this registry pushes it down the open pipe.
 *
 * Thread-safety: ConcurrentHashMap handles concurrent register/push/remove.
 *
 * Scalability note: In a multi-instance deployment, this registry is local to
 * one pod. A Redis Pub/Sub fan-out layer would be needed to reach drivers
 * connected to other instances. For the current single-node setup this is fine.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverSseRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * Registers a new SSE emitter for the given driver.
     * Replaces any existing emitter (driver reconnected).
     */
    public SseEmitter register(String driverId) {
        // 10-minute timeout — driver app reconnects automatically via EventSource
        SseEmitter emitter = new SseEmitter(600_000L);

        emitter.onCompletion(() -> {
            emitters.remove(driverId);
            log.debug("SSE connection completed for driverId={}", driverId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(driverId);
            log.debug("SSE connection timed out for driverId={}", driverId);
        });
        emitter.onError(ex -> {
            emitters.remove(driverId);
            log.warn("SSE error for driverId={}: {}", driverId, ex.getMessage());
        });

        emitters.put(driverId, emitter);
        log.info("Driver SSE registered: driverId={} total_connected={}", driverId, emitters.size());

        // Confirm the stream is live
        try {
            emitter.send(SseEmitter.event().name("connected").data("stream_ready"));
        } catch (IOException e) {
            log.warn("Failed to send connected event to driverId={}", driverId);
        }

        return emitter;
    }

    /**
     * Pushes a delivery offer to the driver's SSE stream.
     *
     * @return true if the driver had an active connection and the event was sent,
     *         false if the driver is not connected (no SSE pipe open).
     */
    public boolean pushOffer(String driverId, OfferPayload offer) {
        SseEmitter emitter = emitters.get(driverId);
        if (emitter == null) {
            log.debug("No SSE connection for driverId={} — skipping offer push", driverId);
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(offer);
            emitter.send(SseEmitter.event().name("delivery_offer").data(json));
            log.info("Offer pushed via SSE: driverId={} orderId={}", driverId, offer.getOrderId());
            return true;
        } catch (IOException e) {
            log.warn("Failed to push offer to driverId={}: {} — removing stale emitter",
                    driverId, e.getMessage());
            emitters.remove(driverId);
            return false;
        }
    }

    /**
     * Pushes an "offer_taken" event to all remaining candidates so their UI
     * can show "This order has already been taken" without waiting for the TTL.
     */
    public void pushOfferTaken(String driverId, String orderId) {
        SseEmitter emitter = emitters.get(driverId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("offer_taken")
                    .data("{\"orderId\":\"" + orderId + "\",\"message\":\"This order has already been taken\"}"));
        } catch (IOException e) {
            emitters.remove(driverId);
        }
    }

    public int connectedCount() {
        return emitters.size();
    }
}
