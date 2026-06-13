package com.foodzie.websocket_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the WebSocket Manager.
 *
 * Authentication is handled at two layers:
 *   1. API Gateway — validates the JWT before proxying the WebSocket upgrade.
 *   2. JwtHandshakeInterceptor — validates the ?token= query param during the
 *      WebSocket handshake, before the connection is established.
 *
 * Because both layers validate the JWT, Spring Security's HTTP layer is kept
 * minimal here — we permit all requests and let the handshake interceptor
 * enforce authentication at the WebSocket level.
 *
 * The /actuator/health endpoint is always open for container orchestration.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // WebSocket upgrade requests and actuator health are permitted at the HTTP layer.
                // Actual authentication is enforced by JwtHandshakeInterceptor.
                .requestMatchers("/ws/**", "/actuator/health").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
