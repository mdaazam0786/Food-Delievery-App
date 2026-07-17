package com.foodzie.delivery_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Dashboard summary of driver earnings and statistics
 */
@Data
@Builder
public class DriverEarningsResponse {
    private String driverId;
    private BigDecimal totalEarnings;
    private Long totalDeliveries;
    private Long activeDeliveries;
    private BigDecimal todaysEarnings;

    // Explicit setter to ensure type safety
    public void setTotalDeliveries(Long totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public void setActiveDeliveries(Long activeDeliveries) {
        this.activeDeliveries = activeDeliveries;
    }
}
