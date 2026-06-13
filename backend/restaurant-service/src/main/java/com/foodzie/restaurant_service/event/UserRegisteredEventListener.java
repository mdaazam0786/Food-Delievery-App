package com.foodzie.restaurant_service.event;

import com.foodzie.restaurant_service.data.Restaurant;
import com.foodzie.restaurant_service.data.RestaurantStatus;
import com.foodzie.restaurant_service.repository.RestaurantRepository;
import com.foodzie.restaurant_service.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the "user.registered" topic and auto-provisions a Restaurant
 * stub whenever a new user registers with the ROLE_RESTAURANT role.
 *
 * <p>The stub is intentionally minimal — the owner fills in the full profile
 * (name, address, images) through the admin panel after first login.
 *
 * <p><b>Idempotent:</b> if a Restaurant already exists for the owner's email
 * (e.g. due to a Kafka redelivery), the event is silently skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private static final String ROLE_RESTAURANT = "ROLE_RESTAURANT";

    private final RestaurantRepository restaurantRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "userRegisteredKafkaListenerContainerFactory"
    )
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: userId={}, email={}, role={}",
                event.getUserId(), event.getEmail(), event.getRole());

        // Only act on restaurant-owner registrations
        if (!ROLE_RESTAURANT.equals(event.getRole())) {
            log.debug("Skipping event for userId={} — role is not ROLE_RESTAURANT", event.getUserId());
            return;
        }

        // Idempotency guard — a restaurant for this email may already exist
        // (duplicate delivery, service restart with earliest offset, etc.)
        if (restaurantRepository.findByOwnerEmail(event.getEmail()).isPresent()) {
            log.warn("Restaurant already exists for ownerEmail={}, skipping auto-provision",
                    event.getEmail());
            return;
        }

        // Build a minimal stub — owner completes the profile via the admin panel
        String restaurantId = IdGenerator.restaurantId();
        String displayName  = (event.getFullName() != null && !event.getFullName().isBlank())
                ? event.getFullName() + "'s Restaurant"
                : "My Restaurant";

        Restaurant stub = Restaurant.builder()
                .id(restaurantId)
                .ownerEmail(event.getEmail())
                .name(displayName)
                .description("")
                .addressText("")
                .latitude(0.0)
                .longitude(0.0)
                .status(RestaurantStatus.CLOSED)   // closed until the owner configures it
                .build();

        restaurantRepository.save(stub);

        log.info("Auto-provisioned Restaurant id={} for ownerEmail={} (userId={})",
                restaurantId, event.getEmail(), event.getUserId());
    }
}
