package com.foodzie.delivery_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sent by the driver's mobile app every ~5 seconds.
 * Triggers a GEOADD + heartbeat SET in Redis.
 * Does NOT touch MySQL.
 */
@Data
public class LocationPingRequest {

    @NotBlank(message = "driverId is required")
    private String driverId;

    @NotNull(message = "latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;

    @NotBlank(message = "cityZone is required")
    private String cityZone;
}
