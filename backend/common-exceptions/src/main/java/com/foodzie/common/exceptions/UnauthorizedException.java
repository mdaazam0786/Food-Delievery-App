package com.foodzie.common.exceptions;

/**
 * Exception thrown when user lacks authentication or authorization.
 * Corresponds to HTTP 401 Unauthorized or 403 Forbidden.
 */
public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public static UnauthorizedException noPermission(String action) {
        return new UnauthorizedException("You do not have permission to " + action);
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid credentials provided");
    }
}
