package com.foodzie.restaurant_service.controller;

import com.foodzie.restaurant_service.data.AdminUser;
import com.foodzie.restaurant_service.repository.AdminUserRepository;
import com.foodzie.restaurant_service.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Analytics endpoints for restaurant admins.
 * Provides business operational performance data metrics.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminUserRepository adminUserRepository;

    /**
     * GET /api/admin/analytics/todays-earnings
     * Returns today's earnings, total orders, and cumulative earnings for the authenticated admin.
     */
    @GetMapping("/todays-earnings")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<Map<String, Object>> getTodaysEarnings(
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        
        AdminUser adminUser = adminUserRepository.findByEmail(email)
                .orElseGet(() -> {
                    AdminUser newAdmin = AdminUser.builder()
                            .id(IdGenerator.adminUserId())
                            .email(email)
                            .build();
                    return adminUserRepository.save(newAdmin);
                });

        Map<String, Object> response = new HashMap<>();
        response.put("todaysEarnings", adminUser.getTodaysEarnings());
        response.put("totalOrders", adminUser.getTotalOrders());
        response.put("totalEarnings", adminUser.getTotalEarnings());

        log.info("Analytics retrieved for email={}", email);
        return ResponseEntity.ok(response);
    }
}
