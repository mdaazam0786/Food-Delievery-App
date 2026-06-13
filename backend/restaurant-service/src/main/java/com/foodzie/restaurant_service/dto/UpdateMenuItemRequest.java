package com.foodzie.restaurant_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/**
 * All fields are optional — only non-null fields are applied (patch semantics).
 */
@Data
public class UpdateMenuItemRequest {

    private String name;

    private String description;

    @DecimalMin(value = "0.01", message = "price must be greater than zero")
    private BigDecimal price;

    private String category;

    private String imageUrl;

    /** Null means "don't change". True/false explicitly sets availability. */
    private Boolean available;

    /** Null means "don't change". True/false explicitly sets vegetarian flag. */
    @JsonProperty("isVeg")
    private Boolean isVeg;
}
