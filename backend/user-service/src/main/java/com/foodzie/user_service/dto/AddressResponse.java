package com.foodzie.user_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AddressResponse {

    private String id;
    private String formattedAddress;
    private String street;
    private String postCode;
    private String apartment;
    private String label;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
