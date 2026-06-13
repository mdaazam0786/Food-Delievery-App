package com.foodzie.websocket_manager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of active WebSocket sessions.
 *
 * Maps orderId → WebSocketSession for the customer currently tracking that order.
 * One order has exactly one active tracking session at a time.
 *
 * Thread-safety: ConcurrentHashMap handles concurrent register/push/close.
 *
 * Scaling note: This registry is local to one pod. The Redis Pub/Sub backplane
 * in RedisLocationBackplane handles cross-pod delivery — when a Kafka ping arrives
 * on a pod that doesn't hold the session, it publishes to Redis, and the pod that
 * does hold the session receives it here and pushes to the WebSocket.
 */
@Slf4j
@Component
public class TrackingSessionRegistry {

    /** orderId → active WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** orderId → userEmail (for ownership validation on reconnect) */
    private final Map<String, String> sessionOwners = new ConcurrentHashMap<>();

    /**
     * Registers a new WebSocket session for the given order.
     * Replaces any existing session (customer reconnected).
     */
    public void register(String orderId, String userEmail, WebSocketSession session) {
        WebSocketSession existing = sessions.put(orderId, session);
        sessionOwners.put(orderId, userEmail);

        // Close the old session gracefully if it was still open
        if (existing != null && existing.isOpen()) {
            try {
                existing.close();
            } catch (IOException e) {
                log.warn("Failed to close stale session for orderId={}: {}", orderId, e.getMessage());
            }
        }

        log.info("WebSocket session registered: orderId={} userEmail={} sessionId={}",
                orderId, userEmail, session.getId());
    }

    /**
     * Pushes a JSON payload to the customer's WebSocket session.
     *
     * @return true if the session was found and the message was sent,
     *         false if no session exists on this pod for this orderId.
     */
    public boolean push(String orderId, String jsonPayload) {
        WebSocketSession session = sessions.get(orderId);
        if (session == null || !session.isOpen()) {
            return false;
        }

        try {
            synchronized (session) {
                // Synchronize on the session — WebSocketSession is not thread-safe
                session.sendMessage(new TextMessage(jsonPayload));
            }
            return true;
        } catch (IOException e) {
            log.warn("Failed to push to WebSocket for orderId={}: {} — removing stale session",
                    orderId, e.getMessage());
            sessions.remove(orderId);
            sessionOwners.remove(orderId);
            return false;
        }
    }

    /**
     * Closes and removes the session for the given order.
     * Called when the order reaches DELIVERED status.
     */
    public void close(String orderId) {
        WebSocketSession session = sessions.remove(orderId);
        sessionOwners.remove(orderId);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage("{\"type\":\"DELIVERY_COMPLETE\"}"));
                session.close();
                log.info("WebSocket session closed (delivery complete): orderId={}", orderId);
            } catch (IOException e) {
                log.warn("Error closing session for orderId={}: {}", orderId, e.getMessage());
            }
        }
    }

    /**
     * Removes a session that was closed by the client (e.g. app backgrounded).
     */
    public void removeIfPresent(String orderId, WebSocketSession session) {
        sessions.remove(orderId, session);
        sessionOwners.remove(orderId);
    }

    public boolean hasSession(String orderId) {
        WebSocketSession s = sessions.get(orderId);
        return s != null && s.isOpen();
    }

    public String getOwner(String orderId) {
        return sessionOwners.get(orderId);
    }

    public int activeCount() {
        return sessions.size();
    }
}
