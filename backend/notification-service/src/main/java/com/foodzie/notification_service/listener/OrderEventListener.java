package com.foodzie.notification_service.listener;

import com.foodzie.notification_service.data.NotificationType;
import com.foodzie.notification_service.event.OrderStatusUpdatedEvent;
import com.foodzie.notification_service.service.NotificationLogService;
import com.foodzie.notification_service.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final PushNotificationService pushService;
    private final NotificationLogService logService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderEventListenerContainerFactory"
    )
    public void onOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        if (!"ORDER_STATUS_UPDATED".equals(event.getEventType())) return;

        log.info("Received OrderStatusUpdatedEvent orderId={} status={}",
                event.getOrderId(), event.getNewStatus());

        String title = null;
        String body = null;
        String template = null;

        switch (event.getNewStatus()) {
            case "PREPARING" -> {
                title = "Order Update 🍳";
                body = "The restaurant has started preparing your food!";
                template = "ORDER_PREPARING";
            }
            case "OUT_FOR_DELIVERY" -> {
                title = "On the way! 🛵";
                body = "Your driver is on the way. Get ready!";
                template = "OUT_FOR_DELIVERY";
            }
            case "DELIVERED" -> {
                title = "Delivered! 🎉";
                body = "Your order has been delivered. Enjoy your meal!";
                template = "ORDER_DELIVERED";
            }
            case "CANCELLED" -> {
                title = "Order Cancelled";
                body = "Your order has been cancelled. Contact support if this was unexpected.";
                template = "ORDER_CANCELLED";
            }
            default -> {
                log.debug("No push notification configured for status={}", event.getNewStatus());
                return;
            }
        }

        // NOTE: In production, fetch the user's FCM token from a user-device-token store.
        // For now we log the intent and record it. Wire up FCM token lookup when ready.
        log.info("Push notification queued for userEmail={} template={}", event.getUserEmail(), template);
        logService.logSuccess(event.getUserEmail(), event.getOrderId(), NotificationType.PUSH, template);

        // Uncomment when FCM token lookup is implemented:
        // String fcmToken = userTokenService.getToken(event.getUserEmail());
        // pushService.send(fcmToken, title, body);
    }
}
