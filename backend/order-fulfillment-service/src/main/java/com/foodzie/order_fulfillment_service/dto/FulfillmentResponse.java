package com.foodzie.order_fulfillment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Returned by all three fulfillment endpoints.
 * Gives the caller confirmation of the transition that just occurred.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentResponse {
    private String orderId;
    private String previousStatus;
    private String newStatus;
    private String driverId;
    private Instant transitionedAt;
}
