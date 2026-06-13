package com.foodzie.restaurant_acceptance_service.controller;

import com.foodzie.restaurant_acceptance_service.service.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE endpoint for the restaurant tablet.
 *
 * The tablet opens this connection when the app starts and keeps it alive.
 * The server pushes new order notifications down this pipe the moment they arrive
 * from Kafka — no polling required.
 *
 * SSE reconnection is handled automatically by the EventSource API on the tablet.
 * If the connection drops, the tablet reconnects and a new emitter is registered.
 *
 * Event types pushed by the server:
 *   connected    — sent immediately on connection to confirm the stream is live
 *   new_order    — sent when a new paid order arrives for this restaurant
 *   order_expired — sent when a pending order times out (5-min TTL)
 */
@Slf4j
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class OrderStreamController {

    private final SseEmitterRegistry sseRegistry;

    /**
     * GET /api/restaurants/{restaurantId}/orders/stream
     * Opens a persistent SSE connection for the given restaurant.
     */
    @GetMapping(value = "/{restaurantId}/orders/stream",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public SseEmitter streamOrders(@PathVariable String restaurantId) {
        log.info("SSE stream requested for restaurantId={}", restaurantId);
        return sseRegistry.register(restaurantId);
    }
}
