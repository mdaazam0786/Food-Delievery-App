package com.foodzie.common.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 * Corresponds to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ResourceNotFoundException of(String resourceType, String identifier) {
        return new ResourceNotFoundException(resourceType + " with identifier '" + identifier + "' not found");
    }

    public static ResourceNotFoundException of(String resourceType, String field, Object value) {
        return new ResourceNotFoundException(resourceType + " with " + field + " '" + value + "' not found");
    }
}
