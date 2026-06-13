package com.foodzie.cart_service.data;

import lombok.*;

import java.math.BigDecimal;

/**
 * Embedded document inside a Cart.
 * Stored as a nested array — no separate collection needed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    /** References the menu item in the catalog service. */
    private String itemId;

    /** Snapshot of the item name at the time it was added. */
    private String itemName;

    private Integer quantity;

    /** Current unit price — fetched from the menu at add time. */
    private BigDecimal unitPrice;
}
