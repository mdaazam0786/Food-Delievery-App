package com.foodzie.order_service.config;

import com.foodzie.order_service.event.DeliveryPartnerAssignedEvent;
import com.foodzie.order_service.event.PaymentCompletedEvent;
import com.foodzie.order_service.event.RestaurantDecisionEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ── Payment events ────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, PaymentCompletedEvent> paymentEventConsumerFactory() {
        JsonDeserializer<PaymentCompletedEvent> deserializer =
                new JsonDeserializer<>(PaymentCompletedEvent.class, false);
        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        // DNS resolution and connection retry for Railway internal network
        props.put("client.dns.lookup", "use_all_dns_ips");
        props.put("reconnect.backoff.ms", 50);
        props.put("reconnect.backoff.max.ms", 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);
        props.put("socket.keepalive.enabled", true);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent>
    paymentEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentEventConsumerFactory());
        // Manual acknowledgment to prevent message loss on connection failure
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        // Disable auto-startup to prevent connection failures on app startup
        // Listeners will connect lazily when messages need to be consumed
        factory.setAutoStartup(false);
        return factory;
    }

    // ── Delivery events ───────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, DeliveryPartnerAssignedEvent> deliveryAssignedConsumerFactory() {
        JsonDeserializer<DeliveryPartnerAssignedEvent> deserializer =
                new JsonDeserializer<>(DeliveryPartnerAssignedEvent.class, false);
        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        // DNS resolution and connection retry for Railway internal network
        props.put("client.dns.lookup", "use_all_dns_ips");
        props.put("reconnect.backoff.ms", 50);
        props.put("reconnect.backoff.max.ms", 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);
        props.put("socket.keepalive.enabled", true);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryPartnerAssignedEvent>
    deliveryAssignedListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryPartnerAssignedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deliveryAssignedConsumerFactory());
        // Manual acknowledgment to prevent message loss on connection failure
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        // Disable auto-startup to prevent connection failures on app startup
        factory.setAutoStartup(false);
        return factory;
    }

    // ── Restaurant decision events ────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, RestaurantDecisionEvent> restaurantDecisionEventConsumerFactory() {
        JsonDeserializer<RestaurantDecisionEvent> deserializer =
                new JsonDeserializer<>(RestaurantDecisionEvent.class, false);
        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        // DNS resolution and connection retry for Railway internal network
        props.put("client.dns.lookup", "use_all_dns_ips");
        props.put("reconnect.backoff.ms", 50);
        props.put("reconnect.backoff.max.ms", 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);
        props.put("socket.keepalive.enabled", true);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RestaurantDecisionEvent>
    restaurantDecisionEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RestaurantDecisionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(restaurantDecisionEventConsumerFactory());
        // Manual acknowledgment to prevent message loss on connection failure
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        // Disable auto-startup to prevent connection failures on app startup
        factory.setAutoStartup(false);
        return factory;
    }
}
