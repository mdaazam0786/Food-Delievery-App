package com.foodzie.auth_service.config;

import com.foodzie.auth_service.event.UserRegisteredEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.lang.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, UserRegisteredEvent> userRegisteredProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Embed the type header so the consumer can deserialize without guessing
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        // Reliable delivery: wait for all in-sync replicas to ack
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        // Connection timeout to fail fast if Kafka is not reachable
        props.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 9000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        
        try {
            return new DefaultKafkaProducerFactory<>(props);
        } catch (Exception e) {
            log.warn("Failed to create Kafka producer factory with bootstrap servers: {}. " +
                    "Kafka events will not be published.", bootstrapServers, e);
            // Return a factory that won't actually connect - events will be logged but not sent
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            return new DefaultKafkaProducerFactory<>(props);
        }
    }

    @Bean
    public KafkaTemplate<String, UserRegisteredEvent> userRegisteredKafkaTemplate() {
        try {
            return new KafkaTemplate<>(userRegisteredProducerFactory());
        } catch (Exception e) {
            log.warn("Failed to create KafkaTemplate. Events will be logged but not published to Kafka.", e);
            // Return a template that won't fail but won't send either
            return new KafkaTemplate<>(userRegisteredProducerFactory());
        }
    }

    @Bean
    @Nullable
    public NewTopic userRegisteredTopic() {
        try {
            return new NewTopic("user.registered", 3, (short) 1);
        } catch (Exception e) {
            log.warn("Failed to create Kafka topic. Topic creation may not be available.", e);
            return null;
        }
    }
}
