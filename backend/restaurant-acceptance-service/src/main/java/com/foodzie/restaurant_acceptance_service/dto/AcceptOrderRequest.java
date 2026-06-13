package com.foodzie.restaurant_acceptance_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcceptOrderRequest {

    @NotNull(message = "estimatedPrepTimeMinutes is required")
    @Min(value = 1,   message = "estimatedPrepTimeMinutes must be at least 1")
    @Max(value = 120, message = "estimatedPrepTimeMinutes must be at most 120")
    private Integer estimatedPrepTimeMinutes;
}
