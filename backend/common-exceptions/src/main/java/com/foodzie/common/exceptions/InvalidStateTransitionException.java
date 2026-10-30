package com.foodzie.common.exceptions;

/**
 * Exception thrown when an invalid state transition is attempted.
 * Examples: Moving order from DELIVERED back to PENDING, invalid driver status change.
 * Corresponds to HTTP 400 Bad Request.
 */
public class InvalidStateTransitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidStateTransitionException of(String currentState, String attemptedState) {
        return new InvalidStateTransitionException("Cannot transition from state '" + currentState + "' to '" + attemptedState + "'");
    }
}
