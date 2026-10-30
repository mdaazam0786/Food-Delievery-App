package com.foodzie.order_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
import com.foodzie.order_service.dto.CreateOrderRequest;
import com.foodzie.order_service.dto.OrderResponse;
import com.foodzie.order_service.dto.UpdateOrderStatusRequest;
import com.foodzie.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            String userEmail = jwt.getSubject();
            log.info("Creating order for user: {}", userEmail);
            OrderResponse response = orderService.createOrder(userEmail, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Order placed successfully", response));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid order creation request: {}", ex.getMessage());
            throw ex;
        } catch (ConflictException ex) {
            log.warn("Conflict creating order: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating order: {}", ex.getMessage(), ex);
            throw ex;
        }
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (page < 0) {
                throw new BadRequestException("Page number cannot be negative");
            }
            if (size < 1 || size > 100) {
                throw new BadRequestException("Page size must be between 1 and 100");
            }

            String userEmail = jwt.getSubject();
            log.debug("Fetching orders for user: {} page={} size={}", userEmail, page, size);
            
            // Cap page size to prevent accidental large fetches
            int safeSize = Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                    page,
                    safeSize,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
            
            Page<OrderResponse> result = orderService.getMyOrders(userEmail, pageable);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request for user orders: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching orders: {}", ex.getMessage(), ex);
            throw ex;
        }
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new BadRequestException("Order ID cannot be empty");
            }

            String userEmail = jwt.getSubject();
            log.debug("Fetching order: {} for user: {}", orderId, userEmail);
            return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(orderId, userEmail)));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request for order: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching order {}: {}", orderId, ex.getMessage(), ex);
            throw ex;
        }
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
        try {
            if (orderId == null || orderId.isBlank()) {
                throw new BadRequestException("Order ID cannot be empty");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }

            log.info("Updating order status: {} with new status: {}", orderId, request.getStatus());
            return ResponseEntity.ok(ApiResponse.ok("Order status updated",
                    orderService.updateOrderStatus(orderId, request)));
        } catch (BadRequestException ex) {
            log.warn("Invalid status update request: {}", ex.getMessage());
            throw ex;
        } catch (InvalidStateTransitionException ex) {
            log.warn("Invalid state transition for order {}: {}", orderId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating order status {}: {}", orderId, ex.getMessage(), ex);
            throw ex;
        }
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

        try {
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new BadRequestException("Restaurant ID cannot be empty");
            }
            if (page < 0) {
                throw new BadRequestException("Page number cannot be negative");
            }
            if (size < 1 || size > 100) {
                throw new BadRequestException("Page size must be between 1 and 100");
            }

            log.debug("Fetching orders for restaurant: {} page={} size={}", restaurantId, page, size);

            // Cap page size to prevent accidental large fetches
            int safeSize = Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                    page,
                    safeSize,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            Page<OrderResponse> result = orderService.getOrdersByRestaurant(restaurantId, pageable);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (BadRequestException ex) {
            log.warn("Invalid request for restaurant orders: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching restaurant orders {}: {}", restaurantId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
