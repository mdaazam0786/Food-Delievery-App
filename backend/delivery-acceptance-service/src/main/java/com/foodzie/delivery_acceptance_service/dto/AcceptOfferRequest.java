package com.foodzie.delivery_acceptance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent by the driver's app when they tap "Accept" on the offer screen.
 */
@Data
public class AcceptOfferRequest {

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotBlank(message = "driverId is required")
    private String driverId;
}
