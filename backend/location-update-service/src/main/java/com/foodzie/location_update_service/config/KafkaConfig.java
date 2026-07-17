package com.foodzie.location_update_service.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration tuned for high-throughput batch consumption.
 *
 * Key settings:
 *   BatchListener = true       — @KafkaListener receives List<String> per poll
 *   fetch.min.bytes = 64KB     — Kafka waits for 64KB before returning a fetch,
 *                                naturally forming batches of ~500 pings
 *   fetch.max.wait.ms = 500    — Maximum wait before returning even if min.bytes
 *                                not reached — matches our 500ms batch window
 *   max.poll.records = 500     — Hard cap on batch size
 *   AckMode = BATCH            — Commits offsets after the full batch is flushed
 *                                to Redis, not after each individual message
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${app.location.max-batch-size}")
    private int maxBatchSize;

    @Value("${app.location.batch-window-ms}")
    private int batchWindowMs;

    @Bean
    public ConsumerFactory<String, String> locationPingConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxBatchSize);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 65536);        // 64 KB
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, batchWindowMs);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // DNS resolution and connection retry for Railway internal network
        props.put("client.dns.lookup", "use_all_dns_ips");
        props.put("reconnect.backoff.ms", 50);
        props.put("reconnect.backoff.max.ms", 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    locationPingListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(locationPingConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setConcurrency(1);
        return factory;
    }

    @Bean
    public NewTopic locationPingsTopic() {
        return new NewTopic("driver-location-pings", 3, (short) 1);
    }
}
