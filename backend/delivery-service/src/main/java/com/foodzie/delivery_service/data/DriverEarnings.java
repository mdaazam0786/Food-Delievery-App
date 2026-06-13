package com.foodzie.delivery_service.data;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks driver earnings and delivery statistics.
 * Updated in real-time when a delivery is completed.
 *
 * Stored in MySQL (driverdb) — single row per driver.
 */
@Entity
@Table(
    name = "driver_earnings",
    indexes = {
        @Index(name = "idx_de_driver_id", columnList = "driver_id", unique = true),
        @Index(name = "idx_de_updated_at", columnList = "updated_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverEarnings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to delivery_partners table
     */
    @Column(name = "driver_id", nullable = false, unique = true, length = 16)
    private String driverId;

    /**
     * Total earnings across all time (sum of completed deliveries)
     * Stored as DECIMAL(10, 2) in DB
     */
    @Column(name = "total_earnings", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    /**
     * Total number of completed deliveries
     */
    @Column(name = "total_deliveries", nullable = false)
    @Builder.Default
    private Long totalDeliveries = 0L;

    /**
     * Number of active/ongoing deliveries (DELIVERING status)
     */
    @Column(name = "active_deliveries", nullable = false)
    @Builder.Default
    private Long activeDeliveries = 0L;

    /**
     * Earnings for today (reset at midnight)
     * Calculated from delivery_history with today's date
     */
    @Column(name = "todays_earnings", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal todaysEarnings = BigDecimal.ZERO;

    /**
     * Last date when earnings were calculated
     */
    @Column(name = "last_earnings_date")
    private LocalDate lastEarningsDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
