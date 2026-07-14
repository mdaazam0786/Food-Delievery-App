package com.foodzie.delivery_matching_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.*;

/**
 * Read-only MongoDB view of the delivery_partners collection.
 * This service never writes to this collection — it only reads driver profiles
 * to verify IDLE status and vehicle type during the matching algorithm.
 */
@Document(collection = "delivery_partners")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartner {

    @Id
    private String id;

    private String fullName;

    private VehicleType vehicleType;

    private KycStatus kycStatus;

    private DriverStatus currentStatus;

    private String cityZone;
}
