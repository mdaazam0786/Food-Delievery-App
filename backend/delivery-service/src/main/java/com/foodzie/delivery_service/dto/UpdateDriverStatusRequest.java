package com.foodzie.delivery_service.dto;

import com.foodzie.delivery_service.data.DriverStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Used for major lifecycle transitions only:
 *   OFFLINE → IDLE   (driver clocks in for the day)
 *   IDLE → OFFLINE   (driver clocks out)
 *   IDLE → DELIVERING (order accepted — usually set by the system, not the driver)
 */
@Data
public class UpdateDriverStatusRequest {

    @NotNull(message = "status is required")
    private DriverStatus status;
}
