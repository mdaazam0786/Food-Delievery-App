package com.foodzie.order_fulfillment_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
import com.foodzie.order_fulfillment_service.dto.FulfillmentResponse;
import com.foodzie.order_fulfillment_service.service.FulfillmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for the physical delivery lifecycle.
 *
 * All endpoints are POST — each call is a one-way state transition, not an idempotent
 * resource update. Using POST makes the intent explicit and avoids confusion with PATCH.
 *
 * POST /api/fulfillment/orders/{orderId}/ready
 *   Actor: Restaurant chef (ROLE_RESTAURANT)
 *   Transition: ACCEPTED → READY_FOR_PICKUP
 *
 * POST /api/fulfillment/orders/{orderId}/pickup
 *   Actor: Driver (ROLE_DRIVER)
 *   Transition: READY_FOR_PICKUP → OUT_FOR_DELIVERY
 *
 * POST /api/fulfillment/orders/{orderId}/deliver
 *   Actor: Driver (ROLE_DRIVER)
 *   Transition: OUT_FOR_DELIVERY → DELIVERED
 */
@Slf4j
@RestController
@RequestMapping("/api/fulfillment/orders")
@RequiredArgsConstructor
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    /**
     * POST /api/fulfillment/orders/{orderId}/ready
     *
     * The chef taps "Food Ready" on the restaurant tablet.
     * Transitions ACCEPTED → READY_FOR_PICKUP.
     * Fires an event that notifies the assigned driver to head to the restaurant.
     */
    @PostMapping("/{orderId}/ready")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<FulfillmentResponse>> markReady(
            @PathVariable String orderId) {
        try {
            log.info("markReady called: orderId={}", orderId);
            
            // Validate input
            if (orderId == null || orderId.isBlank()) {
                log.warn("Order ID is null or blank");
                throw BadRequestException.of("orderId", "Order ID is required");
            }
            
            FulfillmentResponse result = fulfillmentService.markReady(orderId);
            log.info("Order marked as ready for pickup successfully");
            return ResponseEntity.ok(ApiResponse.ok("Order marked as ready for pickup", result));
            
        } catch (BadRequestException | ResourceNotFoundException e) {
            log.warn("Error marking order as ready: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error marking order as ready", e);
            throw new InternalServerException("Failed to mark order as ready", e);
        }
    }

    /**
     * POST /api/fulfillment/orders/{orderId}/pickup
     *
     * The driver taps "Picked Up" after collecting the order from the restaurant.
     * Transitions READY_FOR_PICKUP → OUT_FOR_DELIVERY.
     * Fires an event that notifies the customer their order is on the way.
     */
    @PostMapping("/{orderId}/pickup")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<FulfillmentResponse>> markPickedUp(
            @PathVariable String orderId) {
        try {
            log.info("markPickedUp called: orderId={}", orderId);
            
            // Validate input
            if (orderId == null || orderId.isBlank()) {
                log.warn("Order ID is null or blank");
                throw BadRequestException.of("orderId", "Order ID is required");
            }
            
            FulfillmentResponse result = fulfillmentService.markPickedUp(orderId);
            log.info("Order marked as picked up successfully");
            return ResponseEntity.ok(ApiResponse.ok("Order picked up — en route to customer", result));
            
        } catch (BadRequestException | ResourceNotFoundException e) {
            log.warn("Error marking order as picked up: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error marking order as picked up", e);
            throw new InternalServerException("Failed to mark order as picked up", e);
        }
    }

    /**
     * POST /api/fulfillment/orders/{orderId}/deliver
     *
     * The driver taps "Delivered" after handing the order to the customer.
     * Transitions OUT_FOR_DELIVERY → DELIVERED.
     * Fires the final event that:
     *   - Tells payment-service to settle funds with the restaurant
     *   - Tells the WebSocket manager to close the live tracking connection
     *   - Notifies the customer that their order has arrived
     */
    @PostMapping("/{orderId}/deliver")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<FulfillmentResponse>> markDelivered(
            @PathVariable String orderId) {
        try {
            log.info("markDelivered called: orderId={}", orderId);
            
            // Validate input
            if (orderId == null || orderId.isBlank()) {
                log.warn("Order ID is null or blank");
                throw BadRequestException.of("orderId", "Order ID is required");
            }
            
            FulfillmentResponse result = fulfillmentService.markDelivered(orderId);
            log.info("Order marked as delivered successfully");
            return ResponseEntity.ok(ApiResponse.ok("Order delivered successfully", result));
            
        } catch (BadRequestException | ResourceNotFoundException e) {
            log.warn("Error marking order as delivered: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error marking order as delivered", e);
            throw new InternalServerException("Failed to mark order as delivered", e);
        }
    }
}
