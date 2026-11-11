package com.foodzie.delivery_matching_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Fetches restaurant coordinates from the restaurant-service via Eureka.
 *
 * Restaurant locations change very rarely (only when the owner updates their address),
 * so we cache the result in-process. In production, replace with a distributed
 * Redis cache to share across multiple instances of this service.
 *
 * The restaurant-service public endpoint GET /api/restaurants/{id} returns:
 * { "data": { "latitude": 28.7041, "longitude": 77.1025, ... } }
 * 
 * Retry strategy:
 * - Up to 3 attempts on failure
 * - Exponential backoff: 1s, 2s, 4s
 * - Recovers with IllegalStateException after all attempts exhausted
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantLocationService {

    private final RestTemplate restTemplate;

    /**
     * Returns [latitude, longitude] for the given restaurant.
     * Calls restaurant-service via load-balanced Eureka discovery.
     * 
     * Retries with exponential backoff on failure:
     * - 1st attempt: immediate
     * - 2nd attempt: after 1s
     * - 3rd attempt: after 2s
     *
     * @throws IllegalStateException if all retries exhausted or restaurant not found
     */
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            ),
            retryFor = {Exception.class}
    )
    @SuppressWarnings("unchecked")
    public double[] getLocation(String restaurantId) {
        try {
            String url = "http://restaurant-service/api/restaurants/" + restaurantId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("data") == null) {
                throw new IllegalStateException("Empty response from restaurant-service for id: " + restaurantId);
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            double lat = ((Number) data.get("latitude")).doubleValue();
            double lon = ((Number) data.get("longitude")).doubleValue();

            log.debug("Restaurant location fetched: id={} lat={} lon={}", restaurantId, lat, lon);
            return new double[]{lat, lon};

        } catch (Exception e) {
            log.error("Failed to fetch location for restaurantId={}: {}", restaurantId, e.getMessage());
            throw new IllegalStateException("Cannot resolve restaurant location: " + restaurantId, e);
        }
    }

    /**
     * Recovery method called after all retry attempts are exhausted.
     * Logs the final failure and throws exception to signal caller.
     */
    @Recover
    public double[] recoverGetLocation(IllegalStateException e, String restaurantId) {
        log.error("All retry attempts exhausted for restaurantId={}: {}", restaurantId, e.getMessage());
        throw e;
    }
}
