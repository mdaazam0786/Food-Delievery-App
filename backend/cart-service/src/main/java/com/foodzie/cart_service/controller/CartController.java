package com.foodzie.cart_service.controller;

import com.foodzie.cart_service.dto.AddItemRequest;
import com.foodzie.cart_service.dto.ApiResponse;
import com.foodzie.cart_service.dto.CartResponse;
import com.foodzie.cart_service.dto.UpdateItemRequest;
import com.foodzie.cart_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(jwt.getSubject())));
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
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart",
                cartService.addItem(jwt.getSubject(), request)));
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
        return ResponseEntity.ok(ApiResponse.ok("Cart updated",
                cartService.updateItem(jwt.getSubject(), itemId, request)));
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
        return ResponseEntity.ok(ApiResponse.ok("Item removed",
                cartService.removeItem(jwt.getSubject(), itemId)));
    }

    /**
     * DELETE /api/cart
     * Clears the entire cart — called after a successful checkout.
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal Jwt jwt) {
        cartService.clearCart(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}
