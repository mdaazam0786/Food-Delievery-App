package com.foodzie.delivery_acceptance_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned to the driver's app after an accept attempt.
 *
 * assigned = true  → this driver won the lock, order is theirs
 * assigned = false → another driver was faster; show "Order already taken" UI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptOfferResponse {
    private boolean assigned;
    private String orderId;
    private String driverId;
    private String message;
}
