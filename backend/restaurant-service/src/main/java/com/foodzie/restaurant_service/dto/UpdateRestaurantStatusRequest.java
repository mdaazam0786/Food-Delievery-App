package com.foodzie.restaurant_service.dto;

import com.foodzie.restaurant_service.data.RestaurantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRestaurantStatusRequest {

    @NotNull(message = "status is required")
    private RestaurantStatus status;
}
