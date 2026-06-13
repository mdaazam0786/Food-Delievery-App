package com.foodzie.auth_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    /**
     * Publishes a {@link UserRegisteredEvent} to Kafka.
     * The message key is the userId (String) so all events for the same user
     * land on the same partition and are processed in order.
     */
    public void publishUserRegistered(UserRegisteredEvent event) {
        String key = String.valueOf(event.getUserId());
        CompletableFuture<SendResult<String, UserRegisteredEvent>> future =
                kafkaTemplate.send(userRegisteredTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish UserRegisteredEvent for userId={}: {}",
                        event.getUserId(), ex.getMessage(), ex);
            } else {
                log.info("Published UserRegisteredEvent for userId={} to topic={} partition={} offset={}",
                        event.getUserId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
