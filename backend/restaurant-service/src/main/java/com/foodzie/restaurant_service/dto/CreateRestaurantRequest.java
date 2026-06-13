package com.foodzie.restaurant_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateRestaurantRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotBlank(message = "addressText is required")
    private String addressText;

    @NotNull(message = "latitude is required")
    private Double latitude;

    @NotNull(message = "longitude is required")
    private Double longitude;

    /** Optional — can be set later via a separate image upload flow. */
    private String imageUrl;

    @NotBlank(message = "gstNo is required")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
            message = "Invalid GST number format"
    )
    private String gstNo;

    @NotBlank(message = "fssaiNo is required")
    @Pattern(regexp = "^\\d{14}$", message = "FSSAI must be exactly 14 digits")
    private String fssaiNo;
}
