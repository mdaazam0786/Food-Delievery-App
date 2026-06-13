package com.foodzie.notification_service.data;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    private String id;

    @Field("user_email")
    private String userEmail;

    /** Context — nullable for non-order notifications. */
    @Field("order_id")
    private String orderId;

    private NotificationType type;

    /** e.g. ORDER_CONFIRMATION, OUT_FOR_DELIVERY, PAYMENT_SUCCESS */
    @Field("template_used")
    private String templateUsed;

    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    /** Stores error message if status = FAILED */
    @Field("error_message")
    private String errorMessage;

    @CreatedDate
    @Field("sent_at")
    private LocalDateTime sentAt;
}
