package com.foodzie.cart_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private String id;
    private String userEmail;
    private String restaurantId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;    // sum of all subtotals
    private int totalItems;            // total quantity across all items
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
