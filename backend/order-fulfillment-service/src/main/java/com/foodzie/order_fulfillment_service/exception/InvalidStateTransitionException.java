package com.foodzie.order_fulfillment_service.exception;

/**
 * Thrown when a fulfillment action is attempted on an order that is not in
 * the expected state. For example, calling /ready on an order that is already
 * OUT_FOR_DELIVERY.
 */
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
