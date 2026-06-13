package com.foodzie.user_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    @Size(max = 512)
    private String formattedAddress;

    @Size(max = 255)
    private String street;

    @Size(max = 20)
    private String postCode;

    @Size(max = 100)
    private String apartment;

    /**
     * Must be one of: HOME, WORK, OTHER
     */
    @NotNull(message = "Label is required")
    @Pattern(regexp = "HOME|WORK|OTHER", message = "Label must be HOME, WORK, or OTHER")
    private String label;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;
}
