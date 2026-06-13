package com.foodzie.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirror of payment-service's PaymentCompletedEvent. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private String eventType;
    private String orderId;
    private String userEmail;
    private String status;
    private String transactionId;
}
