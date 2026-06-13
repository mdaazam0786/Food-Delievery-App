package com.foodzie.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitiatePaymentRequest {

    @NotBlank(message = "orderId is required")
    private String orderId;
}
