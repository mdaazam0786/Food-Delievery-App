package com.foodzie.order_service.controller;

import com.foodzie.order_service.dto.ApiResponse;
import com.foodzie.order_service.dto.CreateOrderRequest;
import com.foodzie.order_service.dto.OrderResponse;
import com.foodzie.order_service.dto.UpdateOrderStatusRequest;
import com.foodzie.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/orders
     * Creates a new order. Fires an OrderCreatedEvent to Kafka.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request) {
        String userEmail = jwt.getSubject();
        OrderResponse response = orderService.createOrder(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", response));
    }

    /**
     * GET /api/orders/me?page=0&size=10&sort=createdAt,desc
     * Returns a paginated list of all orders for the authenticated user.
     * Accessible to authenticated users only.
     *
     * Query parameters:
     *   page  — 0-indexed page number (default 0)
     *   size  — items per page (default 10, max 100)
     *   sort  — optional sort field and direction (default: createdAt,desc)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        String userEmail = jwt.getSubject();
        
        // Cap page size to prevent accidental large fetches
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(
                page,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Page<OrderResponse> result = orderService.getMyOrders(userEmail, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * GET /api/orders/{orderId}
     * Returns a specific order. Users can only fetch their own orders.
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId) {
        String userEmail = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(orderId, userEmail)));
    }

    /**
     * PATCH /api/orders/{orderId}/status
     * Updates the order lifecycle status.
     * Intended for admin / restaurant panel / delivery driver use.
     * Fires an OrderStatusUpdatedEvent to Kafka.
     */
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT', 'ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Order status updated",
                orderService.updateOrderStatus(orderId, request)));
    }

    /**
     * GET /api/orders/restaurant/{restaurantId}?page=0&size=20&sort=createdAt,desc
     *
     * Returns a paginated list of all orders for the given restaurant, newest first.
     * Accessible to ROLE_RESTAURANT and ROLE_ADMIN only.
     *
     * Query parameters:
     *   page  — 0-indexed page number (default 0)
     *   size  — items per page (default 20, max 100)
     *   sort  — optional sort field and direction (default: createdAt,desc)
     *
     * Each order in the response carries its MongoDB ObjectId as {@code id},
     * which the frontend uses as the orderId for subsequent status-update calls.
     */
    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByRestaurant(
            @PathVariable String restaurantId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // Cap page size to prevent accidental large fetches
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(
                page,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<OrderResponse> result = orderService.getOrdersByRestaurant(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
