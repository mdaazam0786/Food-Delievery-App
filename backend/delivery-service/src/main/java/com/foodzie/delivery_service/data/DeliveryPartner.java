package com.foodzie.delivery_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "delivery_partners")
@CompoundIndexes({
    @CompoundIndex(name = "status_zone_idx", def = "{'currentStatus': 1, 'cityZone': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryPartner {

    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String phoneNumber;

    @Indexed(unique = true)
    private String email;

    private VehicleType vehicleType;

    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Builder.Default
    private DriverStatus currentStatus = DriverStatus.OFFLINE;

    @Builder.Default
    private String cityZone = "default";

    private LocalDateTime createdAt;
}
