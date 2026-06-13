package com.foodzie.delivery_service.dto;

import com.foodzie.delivery_service.model.DriverLocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyDriversResponse {
    private List<DriverLocation> drivers;
    private int totalActive;
    private String cityZone;
    private double searchRadiusKm;
}
