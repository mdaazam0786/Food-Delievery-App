package com.foodzie.delivery_matching_service.service;

import com.foodzie.delivery_matching_service.data.DeliveryPartner;
import com.foodzie.delivery_matching_service.data.DriverStatus;
import com.foodzie.delivery_matching_service.data.KycStatus;
import com.foodzie.delivery_matching_service.data.VehicleType;
import com.foodzie.delivery_matching_service.event.DeliveryMatchingEvent;
import com.foodzie.delivery_matching_service.model.DriverCandidate;
import com.foodzie.delivery_matching_service.repository.DeliveryPartnerRepository;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core matching algorithm.
 *
 * Step 1 — Geo search (Redis GEOSEARCH):
 *   GEORADIUS drivers:active:{zone} {lon} {lat} {radius} km WITHDIST ASC
 *   Returns driver IDs + straight-line distance from the restaurant.
 *   Distances are captured here and passed through — no second Redis call needed.
 *
 * Step 2 — Heartbeat check (ghost driver filter):
 *   For each candidate, check driver:heartbeat:{id} exists.
 *   Missing = ghost → ZREM from geo set, skip.
 *
 * Step 3 — Profile filter (MySQL bulk fetch):
 *   One query: findAllByIdIn(liveIds, IDLE, APPROVED).
 *   Vehicle filter: BICYCLE excluded for large orders (itemCount > 5).
 *
 * Step 4 — ETA ranking:
 *   ETA = distanceKm / avgSpeedKmh × 60 minutes.
 *   BIKE/SCOOTER = 25 km/h, BICYCLE = 12 km/h.
 *   Sort ascending by ETA.
 *
 * Step 5 — Recursive radius expansion:
 *   3km → 5km → 8km → 10km.
 *   If still 0 after 10km → publish NO_DRIVERS_AVAILABLE.
 *
 * Step 6 — Publish top 5 candidates to delivery-events topic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverMatchingService {

    private static final String GEO_KEY_PREFIX      = "drivers:active:";
    private static final String HEARTBEAT_KEY_PREFIX = "driver:heartbeat:";

    private static final double SPEED_MOTORISED_KMH = 25.0;
    private static final double SPEED_BICYCLE_KMH   = 12.0;

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final DeliveryPartnerRepository driverRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.matching.radius-steps}")
    private String radiusStepsConfig;

    @Value("${app.matching.max-candidates}")
    private int maxCandidates;

    @Value("${app.kafka.topics.delivery-events}")
    private String deliveryEventsTopic;

    // ── Entry point ───────────────────────────────────────────────────────────

    public void findAndDispatch(String orderId, String restaurantId,
                                double restaurantLat, double restaurantLon,
                                String cityZone, int itemCount) {

        double[] radiusSteps = parseRadiusSteps();
        List<DriverCandidate> candidates = Collections.emptyList();
        double usedRadius = 0;

        for (double radius : radiusSteps) {
            log.info("Matching orderId={} restaurantId={} zone={} radius={}km",
                    orderId, restaurantId, cityZone, radius);

            // Step 1: geo search — returns id → distance map
            Map<String, Double> geoHits = searchGeoSet(restaurantLat, restaurantLon, radius, cityZone);
            if (geoHits.isEmpty()) {
                log.debug("No geo hits at {}km — expanding", radius);
                continue;
            }

            // Step 2: ghost filter — removes dead drivers from geo set inline
            Map<String, Double> liveHits = filterGhosts(geoHits, cityZone);
            if (liveHits.isEmpty()) {
                log.debug("All {} geo hits were ghosts at {}km — expanding", geoHits.size(), radius);
                continue;
            }

            // Steps 3 + 4: profile filter + ETA ranking
            candidates = buildCandidates(new ArrayList<>(liveHits.keySet()),
                    liveHits, cityZone, itemCount);

            if (!candidates.isEmpty()) {
                usedRadius = radius;
                break;
            }

            log.debug("No eligible drivers after profile filter at {}km — expanding", radius);
        }

        if (candidates.isEmpty()) {
            publishNoDriversAvailable(orderId, restaurantId, cityZone,
                    radiusSteps[radiusSteps.length - 1]);
        } else {
            List<String> topIds = candidates.stream()
                    .sorted(Comparator.comparingDouble(DriverCandidate::getEtaMinutes))
                    .limit(maxCandidates)
                    .map(DriverCandidate::getDriverId)
                    .collect(Collectors.toList());

            publishMatchingInitiated(orderId, restaurantId, cityZone, topIds, usedRadius);
        }
    }

    // ── Step 1: Redis GEORADIUS WITHDIST ──────────────────────────────────────

    /**
     * Returns a map of driverId → distanceKm for all drivers within the radius.
     * Distances come directly from the GEORADIUS WITHDIST result — no second call.
     */
    private Map<String, Double> searchGeoSet(double lat, double lon,
                                              double radiusKm, String cityZone) {
        String geoKey = GEO_KEY_PREFIX + cityZone;

        Circle searchArea = new Circle(
                new Point(lon, lat),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo().radius(geoKey, searchArea, args);

        if (results == null) return Collections.emptyMap();

        Map<String, Double> hits = new LinkedHashMap<>(); // preserve sort order
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> r : results.getContent()) {
            hits.put(r.getContent().getName(), r.getDistance().getValue());
        }
        return hits;
    }

    // ── Step 2: Ghost driver filter ───────────────────────────────────────────

    private Map<String, Double> filterGhosts(Map<String, Double> candidates, String cityZone) {
        Map<String, Double> live = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : candidates.entrySet()) {
            String driverId = entry.getKey();
            Boolean alive = stringRedisTemplate.hasKey(HEARTBEAT_KEY_PREFIX + driverId);
            if (Boolean.TRUE.equals(alive)) {
                live.put(driverId, entry.getValue());
            } else {
                stringRedisTemplate.opsForZSet().remove(GEO_KEY_PREFIX + cityZone, driverId);
                log.warn("Ghost driver evicted during matching: driverId={}", driverId);
            }
        }
        return live;
    }

    // ── Steps 3 + 4: Profile filter + ETA ranking ────────────────────────────

    private List<DriverCandidate> buildCandidates(List<String> driverIds,
                                                   Map<String, Double> distanceMap,
                                                   String cityZone, int itemCount) {
        // One bulk MySQL query — never N individual lookups
        List<DeliveryPartner> profiles = driverRepository
                .findAllByIdInAndCurrentStatusAndKycStatus(
                        driverIds, DriverStatus.IDLE, KycStatus.APPROVED);

        return profiles.stream()
                // Vehicle filter: no bicycles for large orders
                .filter(d -> itemCount <= 5 || d.getVehicleType() != VehicleType.BICYCLE)
                .map(d -> {
                    double distKm = distanceMap.getOrDefault(d.getId(), 999.0);
                    double speed  = d.getVehicleType() == VehicleType.BICYCLE
                            ? SPEED_BICYCLE_KMH : SPEED_MOTORISED_KMH;
                    double eta    = (distKm / speed) * 60.0;

                    return DriverCandidate.builder()
                            .driverId(d.getId())
                            .vehicleType(d.getVehicleType())
                            .cityZone(cityZone)
                            .distanceKm(distKm)
                            .etaMinutes(eta)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Step 6: Publish events ────────────────────────────────────────────────

    private void publishMatchingInitiated(String orderId, String restaurantId,
                                           String cityZone, List<String> candidateIds,
                                           double radiusKm) {
        DeliveryMatchingEvent event = DeliveryMatchingEvent.builder()
                .eventType("DELIVERY_MATCHING_INITIATED")
                .payload(DeliveryMatchingEvent.Payload.builder()
                        .orderId(orderId)
                        .restaurantId(restaurantId)
                        .cityZone(cityZone)
                        .candidateDriverIds(candidateIds)
                        .searchRadiusKm(radiusKm)
                        .build())
                .build();

        kafkaTemplate.send(deliveryEventsTopic, orderId, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DeliveryMatchingEvent for orderId={}: {}",
                                orderId, ex.getMessage());
                    } else {
                        log.info("DeliveryMatchingEvent published: orderId={} candidates={} radius={}km",
                                orderId, candidateIds.size(), radiusKm);
                    }
                });
    }

    private void publishNoDriversAvailable(String orderId, String restaurantId,
                                            String cityZone, double maxRadius) {
        log.warn("No drivers found for orderId={} after expanding to {}km", orderId, maxRadius);

        DeliveryMatchingEvent event = DeliveryMatchingEvent.builder()
                .eventType("NO_DRIVERS_AVAILABLE")
                .payload(DeliveryMatchingEvent.Payload.builder()
                        .orderId(orderId)
                        .restaurantId(restaurantId)
                        .cityZone(cityZone)
                        .candidateDriverIds(Collections.emptyList())
                        .searchRadiusKm(maxRadius)
                        .failureReason("NO_IDLE_DRIVERS_WITHIN_" + (int) maxRadius + "KM")
                        .build())
                .build();

        kafkaTemplate.send(deliveryEventsTopic, orderId, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish NO_DRIVERS_AVAILABLE for orderId={}: {}",
                                orderId, ex.getMessage());
                    } else {
                        log.warn("NO_DRIVERS_AVAILABLE published for orderId={}", orderId);
                    }
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double[] parseRadiusSteps() {
        return Arrays.stream(radiusStepsConfig.split(","))
                .map(String::trim)
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
