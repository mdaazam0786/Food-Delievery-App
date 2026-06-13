package com.foodzie.delivery_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a driver's current real-time location and status.
 * Sourced from Redis geo queries — not stored in MySQL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocation {

    /** Human-readable driver ID, e.g. "DRV-8832" */
    private String driverId;

    /** City/zone partition key, e.g. "delhi" */
    private String cityZone;

    /** Driver's current latitude */
    private double latitude;

    /** Driver's current longitude */
    private double longitude;

    /** Distance from search origin in kilometers */
    private double distanceKm;
}

