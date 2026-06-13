package com.foodzie.delivery_acceptance_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * High-level summary pushed to each candidate driver's app via SSE.
 * Contains just enough info for the driver to decide whether to accept.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferPayload {
    private String orderId;
    private String restaurantId;

    /** Straight-line distance from the driver to the restaurant (km). */
    private double distanceToRestaurantKm;

    /** Estimated payout for this delivery (placeholder — pricing service TBD). */
    private double estimatedPayout;

    /** Seconds the driver has to respond before the offer expires. */
    private int offerTtlSeconds;
}
