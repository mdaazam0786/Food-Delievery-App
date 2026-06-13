package com.foodzie.user_service.event;

import com.foodzie.user_service.data.UserProfile;
import com.foodzie.user_service.data.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private final UserProfileRepository userProfileRepository;

    /**
     * Consumes {@link UserRegisteredEvent} from the "user.registered" topic and
     * creates a {@link UserProfile} for the newly registered user.
     *
     * <p>Idempotent: if a profile already exists for the given userId (e.g. due to
     * a duplicate delivery), the event is silently skipped.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "userRegisteredKafkaListenerContainerFactory"
    )
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for userId={}, email={}", event.getUserId(), event.getEmail());

        // Idempotency guard — profile may already exist if the event is redelivered
        if (userProfileRepository.findByUserId(event.getUserId()).isPresent()) {
            log.warn("UserProfile already exists for userId={}, skipping creation", event.getUserId());
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(event.getUserId())
                .email(event.getEmail())
                .fullName(event.getFullName())
                .build();

        userProfileRepository.save(profile);
        log.info("Created UserProfile id={} for userId={}", profile.getId(), event.getUserId());
    }
}
