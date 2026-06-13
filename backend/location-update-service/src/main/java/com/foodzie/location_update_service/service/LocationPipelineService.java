package com.foodzie.location_update_service.service;

import com.foodzie.location_update_service.model.LocationPing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Flushes a micro-batch of GPS pings to Redis in a single pipelined network trip.
 *
 * Without pipelining:
 *   500 pings × 2 commands = 1,000 round-trips × 0.5ms = 500ms network overhead
 *
 * With pipelining:
 *   500 pings × 2 commands = 1,000 commands in 1 network trip ≈ 0.5ms total
 *
 * Redis commands per ping:
 *   GEOADD drivers:active:{zone} {lon} {lat} {driverId}
 *   SETEX  driver:heartbeat:{driverId} {ttl} "alive"
 *
 * executePipelined() opens one connection, sends all commands without waiting
 * for individual responses, flushes in one write, reads all responses in one
 * read, then returns the connection to the pool.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationPipelineService {

    private static final String GEO_KEY_PREFIX      = "drivers:active:";
    private static final String HEARTBEAT_KEY_PREFIX = "driver:heartbeat:";
    private static final byte[] ALIVE_BYTES          = "alive".getBytes(StandardCharsets.UTF_8);

    private final RedisTemplate<String, String> stringRedisTemplate;

    @Value("${app.location.heartbeat-ttl-seconds}")
    private long heartbeatTtlSeconds;

    /**
     * Flushes all pings in the batch to Redis in a single pipelined network trip.
     *
     * @param pings the micro-batch of GPS pings from the Kafka consumer poll
     * @return number of pings successfully written
     */
    public int flushBatch(List<LocationPing> pings) {
        if (pings == null || pings.isEmpty()) return 0;

        int written = 0;

        try {
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (LocationPing ping : pings) {
                    if (!isValid(ping)) continue;

                    // GEOADD drivers:active:{zone} {lon} {lat} {driverId}
                    // Redis GEO commands expect longitude first, then latitude.
                    byte[] geoKey = (GEO_KEY_PREFIX + ping.getZone())
                            .getBytes(StandardCharsets.UTF_8);
                    byte[] member = ping.getDriverId().getBytes(StandardCharsets.UTF_8);

                    connection.geoCommands().geoAdd(
                            geoKey,
                            new Point(ping.getLongitude(), ping.getLatitude()),
                            member
                    );

                    // SETEX driver:heartbeat:{driverId} {ttl} "alive"
                    byte[] heartbeatKey = (HEARTBEAT_KEY_PREFIX + ping.getDriverId())
                            .getBytes(StandardCharsets.UTF_8);

                    connection.stringCommands().setEx(
                            heartbeatKey,
                            heartbeatTtlSeconds,
                            ALIVE_BYTES
                    );
                }
                // executePipelined requires returning null from the callback
                return null;
            });

            written = pings.size();

        } catch (Exception e) {
            log.error("Redis pipeline flush failed for batch of {} pings: {}",
                    pings.size(), e.getMessage(), e);
        }

        return written;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Rejects malformed pings before they reach Redis.
     * A bad ping from a buggy client must never crash the pipeline.
     */
    private boolean isValid(LocationPing ping) {
        if (ping.getDriverId() == null || ping.getDriverId().isBlank()) return false;
        if (ping.getZone() == null || ping.getZone().isBlank()) return false;
        if (ping.getLatitude() < -90 || ping.getLatitude() > 90) return false;
        if (ping.getLongitude() < -180 || ping.getLongitude() > 180) return false;
        return true;
    }
}
