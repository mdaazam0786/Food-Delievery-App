package com.foodzie.delivery_matching_service.config;

import com.foodzie.delivery_matching_service.event.OrderStatusUpdatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for delivery-matching-service.
 * 
 * Security considerations:
 * - TRUSTED_PACKAGES restricted to specific packages (not "*")
 * - Manual acknowledgment mode prevents message loss
 * - Retry/backoff configured for resilience
 * - Topic replication factor (should be 3+ for production)
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ── Producer ─────────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer ─────────────────────────────────────────────────────────────

    /**
     * Consumer factory with restricted deserialization packages for security.
     * 
     * SECURITY FIX: Changed from TRUSTED_PACKAGES="*" to specific packages
     * This prevents arbitrary code execution via malicious messages.
     */
    @Bean
    public ConsumerFactory<String, OrderStatusUpdatedEvent> orderStatusConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderStatusUpdatedEvent.class.getName());
        
        // SECURITY: Restrict deserialization to specific event packages
        // Only allow classes from com.foodzie.delivery_matching_service.event package
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.foodzie.delivery_matching_service.event");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        // DNS resolution and connection retry for Railway internal network
        props.put("client.dns.lookup", "use_all_dns_ips");
        props.put("reconnect.backoff.ms", 50);
        props.put("reconnect.backoff.max.ms", 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);
        props.put("socket.keepalive.enabled", true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderStatusUpdatedEvent>
    orderStatusListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderStatusUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderStatusConsumerFactory());
        // Manual acknowledgment to prevent message loss on connection failure
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    // ── Topics ───────────────────────────────────────────────────────────────
    // Replication factor = 3 ensures high availability:
    // - If one broker fails, data is still available on other replicas
    // - At least 2 replicas must be in sync before acknowledging writes
    // For development/testing with single broker, reduce replication factor to 1

    @Bean
    public NewTopic orderEventsTopic() {
        // 3 partitions for parallelism, 3 replicas for fault tolerance
        return new NewTopic("order-events", 3, (short) 3);
    }

    @Bean
    public NewTopic deliveryEventsTopic() {
        // 3 partitions for parallelism, 3 replicas for fault tolerance
        return new NewTopic("delivery-events", 3, (short) 3);
    }
}
