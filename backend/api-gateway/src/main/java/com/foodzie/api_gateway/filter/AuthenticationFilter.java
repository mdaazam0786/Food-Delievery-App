package com.foodzie.api_gateway.filter;

import com.foodzie.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Autowired
    private RouteValidator routeValidator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip filter for public/open endpoints
            if (routeValidator.isSecured.test(request)) {

                // Extract token — prefer Authorization header, fall back to ?token= query param.
                // The query param fallback is required for WebSocket upgrade requests because
                // the WebSocket API (browser and React Native) cannot set custom headers on
                // the initial HTTP Upgrade request.
                String token = extractToken(request);

                if (token == null) {
                    return onError(exchange, "Missing or invalid Authorization", HttpStatus.UNAUTHORIZED);
                }

                try {
                    jwtUtil.validateToken(token);

                    // Forward useful claims as headers to downstream services
                    Claims claims = jwtUtil.extractAllClaims(token);
                    
                    // Log the incoming request for debugging
                    log.info("🔐 AuthenticationFilter: path={} method={} user={}",
                            request.getURI().getPath(), request.getMethod(), claims.getSubject());
                    
                    // Extract roles - handle both "role" (string) and "roles" (array) formats
                    String rolesHeader = "";
                    Object rolesObj = claims.get("roles");
                    if (rolesObj != null) {
                        // Handle roles as array or list
                        if (rolesObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Object> rolesList = (java.util.List<Object>) rolesObj;
                            java.util.List<String> stringRoles = new java.util.ArrayList<>();
                            for (Object role : rolesList) {
                                stringRoles.add(role.toString());
                            }
                            rolesHeader = String.join(",", stringRoles);
                        } else if (rolesObj instanceof String) {
                            rolesHeader = (String) rolesObj;
                        } else {
                            rolesHeader = rolesObj.toString();
                        }
                    } else {
                        // Fall back to "role" (singular) for backward compatibility
                        String singleRole = claims.get("role", String.class);
                        rolesHeader = singleRole != null ? singleRole : "";
                    }
                    
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", claims.getSubject())
                            .header("X-User-Role", rolesHeader)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());

                } catch (JwtException | IllegalArgumentException e) {
                    log.error("🚫 AuthenticationFilter JWT validation failed: {}", e.getMessage());
                    return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
                }
            }

            return chain.filter(exchange);
        };
    }

    /**
     * Extracts the JWT from the request.
     * Priority:
     *   1. Authorization: Bearer {token} header  (REST API calls)
     *   2. ?token={token} query parameter         (WebSocket upgrade requests)
     */
    private String extractToken(ServerHttpRequest request) {
        // 1. Try Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Fall back to query param (WebSocket clients)
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring("token=".length());
                }
            }
        }

        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        byte[] bytes = ("{\"error\": \"" + message + "\"}").getBytes();
        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // placeholder for future per-route config (e.g. required roles)
    }
}
