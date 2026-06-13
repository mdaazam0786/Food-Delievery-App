package com.foodzie.notification_service.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushNotificationService {

    /**
     * Sends a push notification to a specific device via FCM.
     *
     * @param fcmToken  The device token registered by the React Native app
     * @param title     Notification title
     * @param body      Notification body text
     */
    public void send(String fcmToken, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized — skipping push notification to token={}", fcmToken);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent: {} title='{}'", response, title);
        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage());
            throw new RuntimeException("Push notification failed: " + e.getMessage(), e);
        }
    }
}
