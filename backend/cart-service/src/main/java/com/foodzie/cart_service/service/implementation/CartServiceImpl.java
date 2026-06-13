package com.foodzie.cart_service.service.implementation;

import com.foodzie.cart_service.data.Cart;
import com.foodzie.cart_service.data.CartItem;
import com.foodzie.cart_service.dto.*;
import com.foodzie.cart_service.exception.CartConflictException;
import com.foodzie.cart_service.exception.CartNotFoundException;
import com.foodzie.cart_service.repository.CartRepository;
import com.foodzie.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Override
    public CartResponse getCart(String userEmail) {
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userEmail(userEmail).build()
                ));
        return toResponse(cart);
    }

    @Override
    public CartResponse addItem(String userEmail, AddItemRequest request) {
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> Cart.builder().userEmail(userEmail).build());

        // ── Single-restaurant rule ────────────────────────────────────────────
        if (cart.getRestaurantId() != null
                && !cart.getRestaurantId().equals(request.getRestaurantId())) {
            throw new CartConflictException(
                    "Your cart already has items from restaurant '" + cart.getRestaurantId() +
                    "'. Clear your cart before adding items from a different restaurant."
            );
        }

        // Set restaurant on first item
        if (cart.getRestaurantId() == null) {
            cart.setRestaurantId(request.getRestaurantId());
        }

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        // If item already in cart → increment quantity
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getItemId().equals(request.getItemId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + request.getQuantity());
            existing.get().setUnitPrice(request.getUnitPrice()); // refresh price
        } else {
            cart.getItems().add(CartItem.builder()
                    .itemId(request.getItemId())
                    .itemName(request.getItemName())
                    .quantity(request.getQuantity())
                    .unitPrice(request.getUnitPrice())
                    .build());
        }

        Cart saved = cartRepository.save(cart);
        log.info("Item {} added to cart for user={}", request.getItemId(), userEmail);
        return toResponse(saved);
    }

    @Override
    public CartResponse updateItem(String userEmail, String itemId, UpdateItemRequest request) {
        Cart cart = findCart(userEmail);

        if (request.getQuantity() == 0) {
            return removeItem(userEmail, itemId);
        }

        cart.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartNotFoundException("Item not found in cart: " + itemId))
                .setQuantity(request.getQuantity());

        return toResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse removeItem(String userEmail, String itemId) {
        Cart cart = findCart(userEmail);

        boolean removed = cart.getItems().removeIf(i -> i.getItemId().equals(itemId));
        if (!removed) {
            throw new CartNotFoundException("Item not found in cart: " + itemId);
        }

        // If cart is now empty, reset the restaurant lock
        if (cart.getItems().isEmpty()) {
            cart.setRestaurantId(null);
        }

        log.info("Item {} removed from cart for user={}", itemId, userEmail);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    public void clearCart(String userEmail) {
        Cart cart = findCart(userEmail);
        cart.getItems().clear();
        cart.setRestaurantId(null);
        cartRepository.save(cart);
        log.info("Cart cleared for user={}", userEmail);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Cart findCart(String userEmail) {
        return cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userEmail));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream()
                        .map(item -> CartItemResponse.builder()
                                .itemId(item.getItemId())
                                .itemName(item.getItemName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .subtotal(item.getUnitPrice()
                                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                                .build())
                        .toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userEmail(cart.getUserEmail())
                .restaurantId(cart.getRestaurantId())
                .items(itemResponses)
                .totalAmount(total)
                .totalItems(totalItems)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
