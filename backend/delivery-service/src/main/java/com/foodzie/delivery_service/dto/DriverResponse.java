package com.foodzie.delivery_service.dto;

import com.foodzie.delivery_service.data.DriverStatus;
import com.foodzie.delivery_service.data.KycStatus;
import com.foodzie.delivery_service.data.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DriverResponse {
    private String id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private VehicleType vehicleType;
    private KycStatus kycStatus;
    private DriverStatus currentStatus;
    private String cityZone;
    private LocalDateTime createdAt;
}
