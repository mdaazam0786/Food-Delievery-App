package com.foodzie.common.exceptions;

/**
 * Exception thrown when request validation fails or invalid input is provided.
 * Corresponds to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public static BadRequestException of(String fieldName, String reason) {
        return new BadRequestException("Invalid value for field '" + fieldName + "': " + reason);
    }
}
