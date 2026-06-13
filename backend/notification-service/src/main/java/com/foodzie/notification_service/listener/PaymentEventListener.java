package com.foodzie.notification_service.listener;

import com.foodzie.notification_service.data.NotificationType;
import com.foodzie.notification_service.event.PaymentCompletedEvent;
import com.foodzie.notification_service.service.EmailService;
import com.foodzie.notification_service.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final EmailService emailService;
    private final NotificationLogService logService;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentEventListenerContainerFactory"
    )
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent orderId={} status={}", event.getOrderId(), event.getStatus());

        if ("SUCCESS".equals(event.getStatus())) {
            sendPaymentSuccessEmail(event);
        } else if ("FAILED".equals(event.getStatus())) {
            sendPaymentFailedEmail(event);
        }
    }

    private void sendPaymentSuccessEmail(PaymentCompletedEvent event) {
        String template = "PAYMENT_SUCCESS";
        try {
            emailService.send(
                    event.getUserEmail(),
                    "Payment Confirmed – Foodzie",
                    "Hi! Your payment was successful.\n\n" +
                    "Order ID: " + event.getOrderId() + "\n" +
                    "Transaction ID: " + event.getTransactionId() + "\n\n" +
                    "The restaurant has been notified and will start preparing your food shortly.\n\n" +
                    "Thank you for ordering with Foodzie!"
            );
            logService.logSuccess(event.getUserEmail(), event.getOrderId(), NotificationType.EMAIL, template);
        } catch (Exception e) {
            log.error("Failed to send payment success email to {}: {}", event.getUserEmail(), e.getMessage());
            logService.logFailure(event.getUserEmail(), event.getOrderId(), NotificationType.EMAIL, template, e.getMessage());
        }
    }

    private void sendPaymentFailedEmail(PaymentCompletedEvent event) {
        String template = "PAYMENT_FAILED";
        try {
            emailService.send(
                    event.getUserEmail(),
                    "Payment Failed – Foodzie",
                    "Hi! Unfortunately your payment could not be processed.\n\n" +
                    "Order ID: " + event.getOrderId() + "\n\n" +
                    "Please try again or use a different payment method.\n\n" +
                    "Foodzie Support"
            );
            logService.logSuccess(event.getUserEmail(), event.getOrderId(), NotificationType.EMAIL, template);
        } catch (Exception e) {
            log.error("Failed to send payment failed email to {}: {}", event.getUserEmail(), e.getMessage());
            logService.logFailure(event.getUserEmail(), event.getOrderId(), NotificationType.EMAIL, template, e.getMessage());
        }
    }
}
