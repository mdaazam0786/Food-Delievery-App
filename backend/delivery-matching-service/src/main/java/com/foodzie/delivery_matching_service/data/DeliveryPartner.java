package com.foodzie.delivery_matching_service.data;

import jakarta.persistence.*;
import lombok.*;

/**
 * Read-only JPA view of the delivery_partners table in driverdb.
 * This service never writes to this table — it only reads driver profiles
 * to verify IDLE status and vehicle type during the matching algorithm.
 *
 * ddl-auto=none ensures this service never modifies the schema.
 */
@Entity
@Table(name = "delivery_partners")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartner {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status")
    private DriverStatus currentStatus;

    @Column(name = "city_zone")
    private String cityZone;
}
