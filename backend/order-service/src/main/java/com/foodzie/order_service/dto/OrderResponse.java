package com.foodzie.order_service.dto;

import com.foodzie.order_service.data.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private String id;           // MongoDB ObjectId as String
    private String userEmail;
    private String restaurantId;
    private Long deliveryAddressId;
    private String driverId;     // Set after delivery partner is assigned
    private OrderStatus status;
    private BigDecimal foodSubtotal;
    private BigDecimal deliveryFee;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
