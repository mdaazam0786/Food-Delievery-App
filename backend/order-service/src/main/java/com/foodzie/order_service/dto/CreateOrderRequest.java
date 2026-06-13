package com.foodzie.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "restaurantId is required")
    private String restaurantId;

    @NotNull(message = "deliveryAddressId is required")
    private Long deliveryAddressId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    /** Customer's delivery address latitude. */
    @NotNull(message = "customerLatitude is required")
    private Double customerLatitude;

    /** Customer's delivery address longitude. */
    @NotNull(message = "customerLongitude is required")
    private Double customerLongitude;

    /** Restaurant kitchen latitude. */
    @NotNull(message = "restaurantLatitude is required")
    private Double restaurantLatitude;

    /** Restaurant kitchen longitude. */
    @NotNull(message = "restaurantLongitude is required")
    private Double restaurantLongitude;
}
