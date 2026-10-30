package com.foodzie.common.exceptions;

/**
 * Exception thrown when a request conflicts with the current state.
 * Corresponds to HTTP 409 Conflict.
 * 
 * Examples:
 * - User tries to add items from different restaurants to cart (violates single-restaurant rule)
 * - Duplicate resource creation attempt
 * - State transition conflict
 */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ConflictException of(String resource, String reason) {
        return new ConflictException("Conflict with " + resource + ": " + reason);
    }
}
