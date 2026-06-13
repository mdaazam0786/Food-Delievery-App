package com.foodzie.websocket_manager.websocket;

import com.foodzie.websocket_manager.data.Order;
import com.foodzie.websocket_manager.repository.OrderRepository;
import com.foodzie.websocket_manager.service.RedisLocationBackplane;
import com.foodzie.websocket_manager.service.TrackingSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for the live driver tracking endpoint.
 *
 * Endpoint: ws://{host}/ws/track/{orderId}?token={jwt}
 *
 * Connection lifecycle:
 *
 *   afterConnectionEstablished:
 *     1. Extract orderId from the URI path.
 *     2. Extract userEmail from the session attributes (set by HandshakeInterceptor).
 *     3. Load the Order from MongoDB — verify userEmail matches order.userEmail.
 *     4. Register the session in TrackingSessionRegistry.
 *     5. Subscribe this pod to the Redis backplane channel ws:location:{orderId}.
 *     6. Send a "connected" confirmation frame.
 *
 *   afterConnectionClosed:
 *     1. Remove the session from the registry.
 *     2. Unsubscribe from the Redis backplane channel.
 *
 * Location pushes arrive via:
 *   - LocationPingKafkaListener (direct, when this pod holds the session)
 *   - RedisLocationBackplane (cross-pod, when another pod consumed the Kafka ping)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    static final String ATTR_USER_EMAIL = "userEmail";
    static final String ATTR_ORDER_ID   = "orderId";

    private final TrackingSessionRegistry sessionRegistry;
    private final OrderRepository orderRepository;
    private final RedisLocationBackplane backplane;
    private final RedisMessageListenerContainer listenerContainer;

    /**
     * Tracks per-session listener adapters so we can unsubscribe on close.
     * sessionId → MessageListenerAdapter
     */
    private final Map<String, MessageListenerAdapter> sessionListeners = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String orderId   = extractOrderId(session);
        String userEmail = (String) session.getAttributes().get(ATTR_USER_EMAIL);

        if (orderId == null || userEmail == null) {
            log.warn("WebSocket rejected — missing orderId or userEmail: sessionId={}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Load order and verify ownership
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("WebSocket rejected — order not found: orderId={}", orderId);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (!userEmail.equals(order.getUserEmail())) {
            log.warn("WebSocket rejected — ownership mismatch: orderId={} claimedBy={} actualOwner={}",
                    orderId, userEmail, order.getUserEmail());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // Register session locally
        sessionRegistry.register(orderId, userEmail, session);

        // Subscribe to Redis backplane channel for cross-pod delivery
        subscribeToBackplane(session.getId(), orderId);

        // Confirm connection to the client
        session.sendMessage(new TextMessage(
                "{\"type\":\"CONNECTED\",\"orderId\":\"" + orderId + "\"}"
        ));

        log.info("WebSocket connected: orderId={} userEmail={} sessionId={}",
                orderId, userEmail, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String orderId = extractOrderId(session);
        if (orderId != null) {
            sessionRegistry.removeIfPresent(orderId, session);
            unsubscribeFromBackplane(session.getId(), orderId);
            log.info("WebSocket closed: orderId={} status={} sessionId={}",
                    orderId, status, session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error: sessionId={} error={}",
                session.getId(), exception.getMessage());
    }

    // ── Redis backplane subscription ──────────────────────────────────────────

    /**
     * Subscribes this pod to the Redis Pub/Sub channel for the given order.
     * When a message arrives, it is forwarded to RedisLocationBackplane.onBackplaneMessage().
     */
    private void subscribeToBackplane(String sessionId, String orderId) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(backplane, "onBackplaneMessage");
        // The listener method signature: onBackplaneMessage(String orderId, String message)
        // We use a wrapper that injects the orderId from the channel name.
        MessageListenerAdapter wrappedAdapter = new MessageListenerAdapter(
                new BackplaneMessageDelegate(orderId, backplane));

        listenerContainer.addMessageListener(wrappedAdapter,
                new ChannelTopic(backplane.channelFor(orderId)));

        sessionListeners.put(sessionId, wrappedAdapter);
        log.debug("Subscribed to backplane channel: orderId={} sessionId={}", orderId, sessionId);
    }

    private void unsubscribeFromBackplane(String sessionId, String orderId) {
        MessageListenerAdapter adapter = sessionListeners.remove(sessionId);
        if (adapter != null) {
            listenerContainer.removeMessageListener(adapter,
                    new ChannelTopic(backplane.channelFor(orderId)));
            log.debug("Unsubscribed from backplane channel: orderId={} sessionId={}", orderId, sessionId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the orderId from the WebSocket URI path.
     * Expected path: /ws/track/{orderId}
     */
    private String extractOrderId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : null;
        if (path == null) return null;
        // path = /ws/track/abc123
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) return null;
        return path.substring(lastSlash + 1);
    }

    /**
     * Thin delegate that routes Redis backplane messages to the session registry.
     * Needed because MessageListenerAdapter doesn't natively pass the channel name.
     */
    public static class BackplaneMessageDelegate {
        private final String orderId;
        private final RedisLocationBackplane backplane;

        public BackplaneMessageDelegate(String orderId, RedisLocationBackplane backplane) {
            this.orderId   = orderId;
            this.backplane = backplane;
        }

        /** Called by MessageListenerAdapter via reflection when a Redis message arrives. */
        public void handleMessage(String message) {
            backplane.onBackplaneMessage(orderId, message);
        }
    }
}
