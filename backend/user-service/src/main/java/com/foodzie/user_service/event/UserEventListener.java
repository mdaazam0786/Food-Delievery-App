package com.foodzie.user_service.event;

import com.foodzie.auth_service.event.UserRegisteredEvent;
import com.foodzie.user_service.data.UserProfile;
import com.foodzie.user_service.data.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final UserProfileRepository userProfileRepository;

    /**
     * Consumes UserRegisteredEvent from Kafka topic "user.registered"
     * and creates a corresponding UserProfile record.
     */
    @KafkaListener(
            topics = "user.registered",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "userRegisteredKafkaListenerContainerFactory"
    )
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for userId={} email={} role={}",
                event.getUserId(), event.getEmail(), event.getRole());

        try {
            // Check if profile already exists (idempotency)
            if (userProfileRepository.findByUserId(event.getUserId()).isPresent()) {
                log.warn("UserProfile already exists for userId={}. Skipping.", event.getUserId());
                return;
            }

            // Create new UserProfile
            UserProfile profile = UserProfile.builder()
                    .userId(event.getUserId())
                    .email(event.getEmail())
                    .fullName(event.getFullName())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userProfileRepository.save(profile);
            log.info("Created UserProfile for userId={} email={}", event.getUserId(), event.getEmail());

        } catch (Exception e) {
            log.error("Failed to create UserProfile for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            // Don't rethrow - let Kafka handle retry via offset management
        }
    }
}
