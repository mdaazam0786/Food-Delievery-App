package com.foodzie.cart_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateItemRequest {

    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity must be 0 or more (0 removes the item)")
    private Integer quantity;
}
