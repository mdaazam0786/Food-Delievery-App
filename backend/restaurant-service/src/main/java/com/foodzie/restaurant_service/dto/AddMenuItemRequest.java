package com.foodzie.restaurant_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddMenuItemRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.01", message = "price must be greater than zero")
    private BigDecimal price;

    @NotBlank(message = "category is required")
    private String category;

    /** Optional — can be set later via a separate image upload flow. */
    private String imageUrl;

    /** Whether this menu item is vegetarian. */
    @JsonProperty("isVeg")
    private boolean isVeg;
}
