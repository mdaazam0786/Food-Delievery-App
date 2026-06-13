package com.foodzie.delivery_service.service;

import com.foodzie.delivery_service.model.DriverLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages the two Redis data structures that power real-time driver tracking.
 *
 * Structure 1 — Geo Sorted Set (per city/zone):
 *   Key:    drivers:active:{cityZone}   e.g. drivers:active:delhi
 *   Value:  driverId string             e.g. "DRV-8832"
 *   Score:  52-bit geohash (managed by Redis internally)
 *   Command: GEOADD drivers:active:delhi 77.1025 28.7041 "DRV-8832"
 *
 * Structure 2 — Heartbeat Key (per driver):
 *   Key:    driver:heartbeat:{driverId} e.g. driver:heartbeat:DRV-8832
 *   Value:  "alive"
 *   TTL:    15 seconds
 *   Command: SET driver:heartbeat:DRV-8832 "alive" EX 15
 *
 * Ghost Driver Pattern:
 *   When searching for nearby drivers, each candidate's heartbeat key is checked.
 *   If missing (TTL expired), the driver is removed from the geo set (ZREM) and skipped.
 *   This prevents assigning orders to drivers who lost connectivity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationService {

    private static final String GEO_KEY_PREFIX       = "drivers:active:";
    private static final String HEARTBEAT_KEY_PREFIX  = "driver:heartbeat:";
    private static final String HEARTBEAT_VALUE       = "alive";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.driver.heartbeat-ttl-seconds}")
    private long heartbeatTtlSeconds;

    // ── Location ping ─────────────────────────────────────────────────────────

    /**
     * Called every ~5 seconds by the driver's mobile app.
     *
     * Executes two Redis commands:
     *   1. GEOADD drivers:active:{cityZone} {lon} {lat} {driverId}
     *   2. SET driver:heartbeat:{driverId} "alive" EX 15
     *
     * MySQL is NOT touched — this is pure Redis.
     */
    public void updateLocation(String driverId, double lat, double lon, String cityZone) {
        String geoKey       = GEO_KEY_PREFIX + cityZone;
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + driverId;

        // GEOADD — upserts the driver's position in the geo sorted set
        redisTemplate.opsForGeo().add(geoKey, new Point(lon, lat), driverId);

        // SET with TTL — refreshes the heartbeat window
        redisTemplate.opsForValue().set(heartbeatKey, HEARTBEAT_VALUE,
                Duration.ofSeconds(heartbeatTtlSeconds));

        log.debug("Location updated: driverId={} lat={} lon={} zone={}", driverId, lat, lon, cityZone);
    }

    // ── Nearby driver search ──────────────────────────────────────────────────

    /**
     * Finds all active (heartbeat-alive) drivers within {@code radiusKm} of the given point.
     *
     * Algorithm:
     *   1. GEORADIUS drivers:active:{cityZone} {lon} {lat} {radius} km ASC WITHCOORD WITHDIST
     *   2. For each result, check driver:heartbeat:{driverId} exists
     *   3. If heartbeat missing → ghost driver → ZREM from geo set, skip
     *   4. Return only live drivers with their distance
     */
    public List<DriverLocation> findNearbyDrivers(double lat, double lon,
                                                   double radiusKm, String cityZone) {
        String geoKey = GEO_KEY_PREFIX + cityZone;

        Circle searchArea = new Circle(
                new Point(lon, lat),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                redisTemplate.opsForGeo().radius(geoKey, searchArea, args);

        List<DriverLocation> liveDrivers = new ArrayList<>();

        if (geoResults == null) return liveDrivers;

        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : geoResults) {
            String driverId = result.getContent().getName();
            String heartbeatKey = HEARTBEAT_KEY_PREFIX + driverId;

            // Ghost driver check — heartbeat key must exist
            Boolean alive = redisTemplate.hasKey(heartbeatKey);
            if (!Boolean.TRUE.equals(alive)) {
                // Ghost driver: remove from geo set and skip
                evictGhostDriver(driverId, cityZone);
                continue;
            }

            Point coords = result.getContent().getPoint();
            double distKm = result.getDistance().getValue();

            liveDrivers.add(DriverLocation.builder()
                    .driverId(driverId)
                    .cityZone(cityZone)
                    .latitude(coords != null ? coords.getY() : 0)
                    .longitude(coords != null ? coords.getX() : 0)
                    .distanceKm(distKm)
                    .build());
        }

        log.debug("Nearby drivers: zone={} radius={}km found={} (after ghost eviction)",
                cityZone, radiusKm, liveDrivers.size());

        return liveDrivers;
    }

    // ── Driver removal ────────────────────────────────────────────────────────

    /**
     * Removes a driver from the geo set when they go offline or are evicted as a ghost.
     * ZREM drivers:active:{cityZone} {driverId}
     */
    public void removeFromGeoSet(String driverId, String cityZone) {
        String geoKey = GEO_KEY_PREFIX + cityZone;
        redisTemplate.opsForZSet().remove(geoKey, driverId);
        log.debug("Driver removed from geo set: driverId={} zone={}", driverId, cityZone);
    }

    /**
     * Evicts a ghost driver: removes from geo set and deletes the (already-expired) heartbeat key.
     */
    public void evictGhostDriver(String driverId, String cityZone) {
        removeFromGeoSet(driverId, cityZone);
        redisTemplate.delete(HEARTBEAT_KEY_PREFIX + driverId);
        log.warn("Ghost driver evicted: driverId={} zone={}", driverId, cityZone);
    }

    /**
     * Returns all member IDs in the geo set for a given city zone.
     * Used by the cleanup worker to scan for ghost drivers proactively.
     */
    public Set<String> getAllDriversInZone(String cityZone) {
        String geoKey = GEO_KEY_PREFIX + cityZone;
        // ZRANGE 0 -1 returns all members of the sorted set
        return redisTemplate.opsForZSet().range(geoKey, 0, -1);
    }

    public boolean isAlive(String driverId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(HEARTBEAT_KEY_PREFIX + driverId));
    }
}
