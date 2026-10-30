package com.foodzie.common.exceptions;

/**
 * Exception thrown when an external service dependency is unavailable.
 * Corresponds to HTTP 503 Service Unavailable.
 * 
 * Examples: Database connection failure, Kafka broker unavailable, Redis timeout, external API down
 */
public class ServiceUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ServiceUnavailableException of(String serviceName) {
        return new ServiceUnavailableException("Service '" + serviceName + "' is temporarily unavailable. Please try again later");
    }
}
