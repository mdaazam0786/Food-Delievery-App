package com.foodzie.cart_service.service;

import com.foodzie.cart_service.dto.AddItemRequest;
import com.foodzie.cart_service.dto.CartResponse;
import com.foodzie.cart_service.dto.UpdateItemRequest;

public interface CartService {

    /** Returns the user's current cart. Creates an empty one if none exists. */
    CartResponse getCart(String userEmail);

    /**
     * Adds an item to the cart.
     * Enforces the single-restaurant rule:
     *   - If cart is empty → set restaurant and add item.
     *   - If same restaurant → add or increment item.
     *   - If different restaurant → throw CartConflictException.
     */
    CartResponse addItem(String userEmail, AddItemRequest request);

    /**
     * Updates the quantity of a specific item.
     * Setting quantity to 0 removes the item.
     */
    CartResponse updateItem(String userEmail, String itemId, UpdateItemRequest request);

    /** Removes a single item from the cart. */
    CartResponse removeItem(String userEmail, String itemId);

    /** Clears all items and resets the restaurant — called after checkout. */
    void clearCart(String userEmail);
}
