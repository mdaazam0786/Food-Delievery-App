package com.foodzie.delivery_service.dto;

import com.foodzie.delivery_service.data.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Single delivery record for history and earnings pages
 */
@Data
@Builder
public class DeliveryHistoryResponse {
    private String id;
    private String orderId;
    private String restaurantName;
    private String pickupAddress;
    private String deliveryAddress;
    private BigDecimal payoutAmount;
    private DeliveryStatus deliveryStatus;
    private Integer customerRating;
    private String customerFeedback;
    private LocalDateTime completedAt;

    // Explicit setter to ensure type safety
    public void setId(String id) {
        this.id = id;
    }
}
