package com.foodzie.auth_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published to Kafka topic "user.registered" immediately after a new user is
 * persisted in auth-service.  user-service consumes this event to eagerly
 * create the corresponding UserProfile row.  restaurant-service consumes it
 * to auto-provision a Restaurant stub for ROLE_RESTAURANT registrations.
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
