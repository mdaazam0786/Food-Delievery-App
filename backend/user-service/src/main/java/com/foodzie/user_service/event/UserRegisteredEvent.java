package com.foodzie.user_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event consumed from Kafka topic "user.registered" when a new user registers
 * in auth-service. This mirrors the auth-service event class to allow
 * deserialization in the user-service consumer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    /** auth-service users.id */
    private String userId;

    private String email;

    /** May be null if the user did not supply one during registration. */
    private String fullName;

    /**
     * The role assigned at registration time.
     * Values: "ROLE_USER" | "ROLE_RESTAURANT" | "ROLE_DRIVER"
     * Consumers that only care about a specific role should filter on this field.
     */
    private String role;
}
