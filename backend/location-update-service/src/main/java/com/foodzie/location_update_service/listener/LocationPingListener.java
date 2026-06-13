package com.foodzie.location_update_service.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodzie.location_update_service.model.LocationPing;
import com.foodzie.location_update_service.service.LocationPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch Kafka listener for the driver-location-pings topic.
 *
 * Receives a List<String> per poll (batch mode enabled in KafkaConfig).
 * Deserializes each message into a LocationPing, then hands the entire
 * batch to LocationPipelineService for a single pipelined Redis flush.
 *
 * Kafka offset commit happens after the pipeline flush succeeds (AckMode.BATCH).
 * If the flush fails, the batch is not committed and will be reprocessed.
 *
 * Partition strategy:
 *   The driver-location-pings topic should be partitioned by city_zone.
 *   Each partition is consumed by one thread (concurrency=1 per partition).
 *   To scale, add partitions and increase the concurrency setting in KafkaConfig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationPingListener {

    private final LocationPipelineService pipelineService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.location-pings}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "locationPingListenerContainerFactory"
    )
    public void onLocationPings(List<String> rawMessages) {
        if (rawMessages == null || rawMessages.isEmpty()) return;

        long start = System.currentTimeMillis();

        // Deserialize all messages — skip malformed ones, never crash the batch
        List<LocationPing> pings = new ArrayList<>(rawMessages.size());
        int skipped = 0;

        for (String raw : rawMessages) {
            try {
                pings.add(objectMapper.readValue(raw, LocationPing.class));
            } catch (JsonProcessingException e) {
                skipped++;
                log.warn("Skipping malformed ping: {} — {}", raw, e.getMessage());
            }
        }

        // Flush the entire batch to Redis in one pipelined network trip
        int written = pipelineService.flushBatch(pings);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Batch processed: total={} written={} skipped={} elapsed={}ms",
                rawMessages.size(), written, skipped, elapsed);
    }
}
