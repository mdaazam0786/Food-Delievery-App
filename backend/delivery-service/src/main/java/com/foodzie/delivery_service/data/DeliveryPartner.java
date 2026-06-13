package com.foodzie.delivery_service.data;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Source of truth for driver identity and lifecycle state.
 * Stored in MySQL (driverdb) — handles slow-moving data only.
 *
 * Architectural rule: current_status is updated only on major lifecycle events
 * (clock-in, order accept, clock-out). It is NEVER updated on every GPS ping.
 * Real-time location lives exclusively in Redis.
 */
@Entity
@Table(
    name = "delivery_partners",
    indexes = {
        @Index(name = "idx_dp_phone",  columnList = "phone_number", unique = true),
        @Index(name = "idx_dp_email",  columnList = "email", unique = true),
        @Index(name = "idx_dp_status", columnList = "current_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryPartner {

    /**
     * Human-readable ID assigned at onboarding, e.g. "DRV-8832".
     * Stored as VARCHAR — not auto-incremented so we control the format.
     */
    @Id
    @Column(name = "id", length = 16, nullable = false, updatable = false)
    private String id;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 16)
    private String phoneNumber;

    @Column(name = "email", nullable = false, unique = true, length = 128)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 16)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 16)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    /**
     * Lifecycle status — updated only on major events, not on GPS pings.
     * Real-time availability is determined by the Redis heartbeat key.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 16)
    @Builder.Default
    private DriverStatus currentStatus = DriverStatus.OFFLINE;

    /**
     * City/zone key used to partition the Redis geo set.
     * e.g. "delhi" → Redis key: drivers:active:delhi
     */
    @Column(name = "city_zone", nullable = false, length = 32)
    @Builder.Default
    private String cityZone = "default";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
