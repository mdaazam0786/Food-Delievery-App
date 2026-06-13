package com.foodzie.delivery_matching_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
     * @throws IllegalStateException if the restaurant cannot be found or the call fails
     */
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
}
