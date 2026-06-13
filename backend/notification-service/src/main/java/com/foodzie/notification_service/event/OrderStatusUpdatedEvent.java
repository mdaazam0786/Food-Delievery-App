package com.foodzie.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirror of order-service's OrderStatusUpdatedEvent. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {
    private String eventType;
    private String orderId;
    private String userEmail;
    private String newStatus;
}
