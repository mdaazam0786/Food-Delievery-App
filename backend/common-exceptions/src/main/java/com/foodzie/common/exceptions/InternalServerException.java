package com.foodzie.common.exceptions;

/**
 * Exception thrown for internal server errors that are not recoverable by the client.
 * Corresponds to HTTP 500 Internal Server Error.
 */
public class InternalServerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InternalServerException(String message) {
        super(message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InternalServerException of(String operationName, String reason) {
        return new InternalServerException("Failed to " + operationName + ": " + reason);
    }
}
