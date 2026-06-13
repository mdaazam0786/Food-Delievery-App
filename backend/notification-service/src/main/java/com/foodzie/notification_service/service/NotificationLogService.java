package com.foodzie.notification_service.service;

import com.foodzie.notification_service.data.NotificationLog;
import com.foodzie.notification_service.data.NotificationStatus;
import com.foodzie.notification_service.data.NotificationType;
import com.foodzie.notification_service.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationLogService {

    private final NotificationLogRepository repository;

    public void logSuccess(String userEmail, String orderId,
                           NotificationType type, String template) {
        repository.save(NotificationLog.builder()
                .userEmail(userEmail)
                .orderId(orderId)
                .type(type)
                .templateUsed(template)
                .status(NotificationStatus.SENT)
                .build());
    }

    public void logFailure(String userEmail, String orderId,
                           NotificationType type, String template, String error) {
        repository.save(NotificationLog.builder()
                .userEmail(userEmail)
                .orderId(orderId)
                .type(type)
                .templateUsed(template)
                .status(NotificationStatus.FAILED)
                .errorMessage(error)
                .build());
    }
}
