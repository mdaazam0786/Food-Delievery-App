package com.foodzie.delivery_matching_service.model;

import com.foodzie.delivery_matching_service.data.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Intermediate model used during the matching algorithm.
 * Combines Redis geo data (distance) with MySQL profile data (vehicle type, status).
 * Never persisted — lives only for the duration of one matching run.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverCandidate {

    private String driverId;
    private VehicleType vehicleType;
    private String cityZone;

    /** Straight-line distance from the restaurant in km (from Redis GEOSEARCH). */
    private double distanceKm;

    /**
     * Estimated Time of Arrival to the restaurant in minutes.
     * Approximated as distanceKm / avgSpeedKmh × 60.
     * Ranking is done on ETA, not raw distance.
     */
    private double etaMinutes;
}
