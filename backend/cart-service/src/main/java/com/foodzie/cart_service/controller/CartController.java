package com.foodzie.cart_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
import com.foodzie.cart_service.dto.AddItemRequest;
import com.foodzie.cart_service.dto.CartResponse;
import com.foodzie.cart_service.dto.UpdateItemRequest;
import com.foodzie.cart_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * GET /api/cart
     * Returns the current user's cart. Creates an empty one if none exists.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            String userEmail = jwt.getSubject();
            log.debug("Fetching cart for user: {}", userEmail);
            return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userEmail)));
        } catch (UnauthorizedException ex) {
            log.warn("Unauthorized access: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching cart: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * POST /api/cart/items
     * Adds an item to the cart.
     * Enforces the single-restaurant rule — throws 409 if restaurant differs.
     */
    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddItemRequest request) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            String userEmail = jwt.getSubject();
            log.info("Adding item to cart for user: {}", userEmail);
            return ResponseEntity.ok(ApiResponse.ok("Item added to cart",
                    cartService.addItem(userEmail, request)));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (ConflictException ex) {
            log.warn("Conflict adding item to cart: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error adding item to cart: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * PATCH /api/cart/items/{itemId}
     * Updates the quantity of a specific item.
     * Sending quantity=0 removes the item.
     */
    @PatchMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (itemId == null || itemId.isBlank()) {
                throw new BadRequestException("Item ID cannot be empty");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            String userEmail = jwt.getSubject();
            log.info("Updating cart item: {} for user: {}", itemId, userEmail);
            return ResponseEntity.ok(ApiResponse.ok("Cart updated",
                    cartService.updateItem(userEmail, itemId, request)));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating cart item {}: {}", itemId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * DELETE /api/cart/items/{itemId}
     * Removes a specific item from the cart.
     */
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (itemId == null || itemId.isBlank()) {
                throw new BadRequestException("Item ID cannot be empty");
            }
            String userEmail = jwt.getSubject();
            log.info("Removing cart item: {} for user: {}", itemId, userEmail);
            return ResponseEntity.ok(ApiResponse.ok("Item removed",
                    cartService.removeItem(userEmail, itemId)));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error removing cart item {}: {}", itemId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * DELETE /api/cart
     * Clears the entire cart — called after a successful checkout.
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            String userEmail = jwt.getSubject();
            log.info("Clearing cart for user: {}", userEmail);
            cartService.clearCart(userEmail);
            return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
        } catch (UnauthorizedException ex) {
            log.warn("Unauthorized access: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error clearing cart: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
