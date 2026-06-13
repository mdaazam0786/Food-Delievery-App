package com.foodzie.delivery_service.data;

public enum DriverStatus {
    OFFLINE,      // Not on shift
    IDLE,         // On shift, waiting for an order
    DELIVERING    // Currently on a delivery run
}
