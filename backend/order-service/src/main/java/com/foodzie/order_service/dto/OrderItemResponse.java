package com.foodzie.order_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private String menuItemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
}
