package com.foodzie.order_service.data;

import lombok.*;

import java.math.BigDecimal;

/**
 * Embedded document inside an Order — stored as a nested array in MongoDB.
 * No separate collection needed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    /** References the menu item in the catalog service. */
    private String menuItemId;

    /** Snapshot of the item name at the time of order. */
    private String itemName;

    private Integer quantity;

    /**
     * Price per unit at the time of purchase.
     * Always a snapshot — never reference the live menu price.
     */
    private BigDecimal priceAtPurchase;
}
