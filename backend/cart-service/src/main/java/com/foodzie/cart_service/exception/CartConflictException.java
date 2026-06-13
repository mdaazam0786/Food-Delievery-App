package com.foodzie.cart_service.exception;

/**
 * Thrown when a user tries to add an item from a different restaurant
 * than the one already in their cart.
 */
public class CartConflictException extends RuntimeException {
    public CartConflictException(String message) {
        super(message);
    }
}
