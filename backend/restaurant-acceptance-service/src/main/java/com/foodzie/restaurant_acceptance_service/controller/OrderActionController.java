package com.foodzie.restaurant_acceptance_service.controller;

import com.foodzie.restaurant_acceptance_service.dto.AcceptOrderRequest;
import com.foodzie.restaurant_acceptance_service.dto.ApiResponse;
import com.foodzie.restaurant_acceptance_service.dto.DeclineOrderRequest;
import com.foodzie.restaurant_acceptance_service.service.OrderAcceptanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Action endpoints for the kitchen staff tablet.
 *
 * POST /api/restaurants/orders/{orderId}/accept
 *   Chef taps "Accept" — provides estimated prep time.
 *   Fires RestaurantDecisionEvent with newStatus = ACCEPTED.
 *
 * POST /api/restaurants/orders/{orderId}/decline
 *   Chef taps "Decline" — provides a reason.
 *   Fires RestaurantDecisionEvent with newStatus = DECLINED.
 */
@RestController
@RequestMapping("/api/restaurants/orders")
@RequiredArgsConstructor
public class OrderActionController {

    private final OrderAcceptanceService acceptanceService;

    /**
     * POST /api/restaurants/orders/{orderId}/accept
     */
    @PostMapping("/{orderId}/accept")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<Void>> acceptOrder(
            @PathVariable String orderId,
            @Valid @RequestBody AcceptOrderRequest request) {
        acceptanceService.acceptOrder(orderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Order accepted"));
    }

    /**
     * POST /api/restaurants/orders/{orderId}/decline
     */
    @PostMapping("/{orderId}/decline")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<Void>> declineOrder(
            @PathVariable String orderId,
            @Valid @RequestBody DeclineOrderRequest request) {
        acceptanceService.declineOrder(orderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Order declined"));
    }
}
