package com.foodzie.order_fulfillment_service.data;

/**
 * Mirror of order-service's OrderStatus enum.
 * Must stay in sync with order-service/data/OrderStatus.java.
 */
public enum OrderStatus {
    PENDING,
    PAYMENT_FAILED,
    PREPARING,
    ACCEPTED,
    DECLINED,
    READY_FOR_PICKUP,   // Chef marked food ready
    OUT_FOR_DELIVERY,   // Driver picked up
    DELIVERED,          // Driver confirmed delivery
    CANCELLED
}
