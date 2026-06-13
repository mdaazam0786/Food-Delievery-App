package com.foodzie.websocket_manager.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodzie.websocket_manager.model.LocationPing;
import com.foodzie.websocket_manager.model.TrackingPayload;
import com.foodzie.websocket_manager.service.ActiveDeliveryRegistry;
import com.foodzie.websocket_manager.service.RedisLocationBackplane;
import com.foodzie.websocket_manager.service.TrackingSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Batch Kafka consumer for the driver-location-pings topic.
 *
 * For each ping in the batch:
 *   1. Look up which orderId this driverId is currently assigned to.
 *      (ActiveDeliveryRegistry maintains a driverId → orderId map, kept in sync
 *       by consuming order-events for OUT_FOR_DELIVERY and DELIVERED transitions.)
 *   2. If no active order for this driver → skip (99% of pings are filtered here).
 *   3. Build a lean TrackingPayload (lat, lon, ts).
 *   4. Try to push directly to the local WebSocket session (fast path).
 *   5. If no local session → publish to Redis backplane (cross-pod delivery).
 *
 * Batch mode is used (same as location-update-service) to amortize Kafka poll overhead.
 * The consumer group is separate from location-update-service so both services
 * independently consume every ping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationPingKafkaListener {

    private final ObjectMapper objectMapper;
    private final ActiveDeliveryRegistry deliveryRegistry;
    private final TrackingSessionRegistry sessionRegistry;
    private final RedisLocationBackplane backplane;

    @KafkaListener(
            topics = "${app.kafka.topics.location-pings}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "locationPingListenerContainerFactory"
    )
    public void onLocationPings(List<String> rawMessages) {
        if (rawMessages == null || rawMessages.isEmpty()) return;

        for (String raw : rawMessages) {
            LocationPing ping;
            try {
                ping = objectMapper.readValue(raw, LocationPing.class);
            } catch (JsonProcessingException e) {
                log.warn("Skipping malformed ping: {}", e.getMessage());
                continue;
            }

            // ── Filter: only process pings for drivers on active deliveries ──
            String orderId = deliveryRegistry.getOrderId(ping.getDriverId());
            if (orderId == null) {
                // This driver has no active delivery — skip (the common case)
                continue;
            }

            // ── Build lean payload ────────────────────────────────────────────
            TrackingPayload payload = TrackingPayload.builder()
                    .lat(ping.getLatitude())
                    .lon(ping.getLongitude())
                    .ts(ping.getTimestamp())
                    .build();

            String json;
            try {
                json = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize TrackingPayload for orderId={}", orderId);
                continue;
            }

            // ── Fast path: push directly if this pod holds the session ────────
            boolean pushed = sessionRegistry.push(orderId, json);

            if (!pushed) {
                // ── Slow path: broadcast to Redis backplane ───────────────────
                // Another pod may hold the session. All pods are subscribed to
                // ws:location:{orderId} — the one with the session will push it.
                backplane.publish(orderId, json);
                log.debug("Ping forwarded to backplane: orderId={} driverId={}",
                        orderId, ping.getDriverId());
            }
        }
    }
}
