package com.foodzie.user_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirror of auth-service's UserRegisteredEvent.
 * Consumed from Kafka topic "user.registered" to eagerly create a UserProfile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private String userId;
    private String email;
    private String fullName;
}
