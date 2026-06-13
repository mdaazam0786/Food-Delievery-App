package com.foodzie.restaurant_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirror of auth-service's UserRegisteredEvent.
 *
 * Consumed from the "user.registered" Kafka topic.
 * restaurant-service only acts on events where role == "ROLE_RESTAURANT".
 *
 * Field names must match the JSON keys produced by auth-service exactly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    /** auth-service users.id */
    private Long userId;

    private String email;

    /** May be null if the user did not supply one during registration. */
    private String fullName;

    /**
     * Role assigned at registration.
     * Values: "ROLE_USER" | "ROLE_RESTAURANT" | "ROLE_DRIVER"
     */
    private String role;
}
