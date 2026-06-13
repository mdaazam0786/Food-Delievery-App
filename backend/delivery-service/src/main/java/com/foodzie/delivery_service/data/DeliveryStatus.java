package com.foodzie.delivery_service.data;

/**
 * Status of a delivery lifecycle.
 */
public enum DeliveryStatus {
    PENDING,      // Order accepted, waiting for pickup
    PICKED_UP,    // Package picked up from restaurant
    IN_TRANSIT,   // En route to customer
    COMPLETED,    // Delivered to customer
    FAILED,       // Failed to deliver
    CANCELLED     // Cancelled by system or driver
}
