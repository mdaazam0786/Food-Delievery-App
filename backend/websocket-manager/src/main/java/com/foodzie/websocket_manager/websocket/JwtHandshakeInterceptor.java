package com.foodzie.websocket_manager.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Validates the JWT during the WebSocket handshake (HTTP Upgrade request).
 *
 * Why query param instead of Authorization header?
 * The WebSocket API in browsers and React Native does not allow setting custom
 * headers on the initial HTTP Upgrade request. The standard workaround is to
 * pass the token as a query parameter: ws://host/ws/track/{orderId}?token={jwt}
 *
 * Security note: The token is only present in the initial HTTP Upgrade request,
 * not in subsequent WebSocket frames. TLS (wss://) ensures it is not exposed
 * in transit. The gateway also validates the token before proxying.
 *
 * On success: sets "userEmail" in the WebSocket session attributes.
 * On failure: returns false → Spring rejects the handshake with 401.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query == null) {
            log.warn("WebSocket handshake rejected — no query string");
            return false;
        }

        String token = extractToken(query);
        if (token == null) {
            log.warn("WebSocket handshake rejected — no token param");
            return false;
        }

        try {
            JwtDecoder decoder = buildDecoder();
            var jwt = decoder.decode(token);
            String userEmail = jwt.getSubject();

            if (userEmail == null || userEmail.isBlank()) {
                log.warn("WebSocket handshake rejected — JWT has no subject");
                return false;
            }

            attributes.put(TrackingWebSocketHandler.ATTR_USER_EMAIL, userEmail);
            log.debug("WebSocket handshake accepted: userEmail={}", userEmail);
            return true;

        } catch (Exception e) {
            log.warn("WebSocket handshake rejected — invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No post-handshake action needed
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractToken(String query) {
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring("token=".length());
            }
        }
        return null;
    }

    private JwtDecoder buildDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
