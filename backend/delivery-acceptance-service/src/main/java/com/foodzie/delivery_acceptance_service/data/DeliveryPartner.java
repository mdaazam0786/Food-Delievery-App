package com.foodzie.delivery_acceptance_service.data;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Read/write mirror of the delivery_partners table in driverdb.
 * This service only updates current_status (IDLE → DELIVERING) on assignment.
 * All other fields are managed by delivery-service.
 */
@Entity
@Table(name = "delivery_partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryPartner {

    @Id
    @Column(name = "id", length = 16, nullable = false, updatable = false)
    private String id;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 16)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 16)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 16)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 16)
    @Builder.Default
    private DriverStatus currentStatus = DriverStatus.OFFLINE;

    @Column(name = "city_zone", nullable = false, length = 32)
    @Builder.Default
    private String cityZone = "default";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
