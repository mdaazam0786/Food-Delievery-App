package com.foodzie.order_fulfillment_service.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter to extract authentication from headers set by the API Gateway.
 * 
 * The API Gateway (running on port 8080) validates JWT tokens and sets:
 *   X-User-Id: The subject (user email) from the JWT
 *   X-User-Role: The role from the JWT (e.g., "ROLE_ADMIN", "ROLE_RESTAURANT", "ROLE_DRIVER")
 * 
 * This filter reads those headers and creates a Spring Security Authentication
 * so that @PreAuthorize annotations work correctly in downstream services.
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        // Only process if headers are present (gateway has already authenticated)
        if (userId != null && !userId.isEmpty()) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            
            if (userRole != null && !userRole.isEmpty()) {
                // Parse comma-separated roles if present
                for (String role : userRole.split(",")) {
                    String trimmedRole = role.trim();
                    if (!trimmedRole.isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority(trimmedRole));
                    }
                }
            }

            // Create authentication token from headers
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            logger.debug("🔐 HeaderAuthenticationFilter: userId=" + userId + " roles=" + userRole);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
