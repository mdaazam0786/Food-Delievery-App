package com.foodzie.restaurant_acceptance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeclineOrderRequest {

    @NotBlank(message = "reason is required")
    private String reason;  // e.g. "OUT_OF_STOCK", "RESTAURANT_CLOSING", "ITEM_UNAVAILABLE"
}
