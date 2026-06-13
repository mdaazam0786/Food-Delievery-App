package com.foodzie.order_service.data;

public enum OrderStatus {
    PENDING,
    PAYMENT_FAILED,
    PREPARING,
    ACCEPTED,           // Restaurant confirmed the order
    DECLINED,           // Restaurant declined (or 5-min timeout)
    READY_FOR_PICKUP,   // Chef marked food ready — driver heads to restaurant
    OUT_FOR_DELIVERY,   // Driver picked up — en route to customer
    DELIVERED,          // Driver confirmed delivery — triggers payment settlement
    CANCELLED
}
