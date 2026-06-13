package com.foodzie.websocket_manager.config;

import com.foodzie.websocket_manager.websocket.JwtHandshakeInterceptor;
import com.foodzie.websocket_manager.websocket.TrackingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the WebSocket endpoint.
 *
 * Endpoint: /ws/track/{orderId}
 *   - The {orderId} segment is part of the path, not a query param.
 *     Spring WebSocket doesn't support path variable extraction natively,
 *     so we register a wildcard pattern and extract the orderId in the handler.
 *   - The JWT token is passed as a query param: ?token={jwt}
 *
 * setAllowedOrigins("*") is intentionally permissive for development.
 * In production, restrict to the app's domain.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TrackingWebSocketHandler trackingHandler;
    private final JwtHandshakeInterceptor jwtInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(trackingHandler, "/ws/track/**")
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins("*");
    }
}
