package com.foodzie.api_gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    /**
     * Endpoints that do NOT require a JWT token.
     */
    public static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/v1/auth",
            "/oauth2",
            "/login/oauth2"
    );

    /**
     * Returns true if the request targets a secured (non-public) route.
     */
    public Predicate<ServerHttpRequest> isSecured =
            request -> OPEN_ENDPOINTS.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
