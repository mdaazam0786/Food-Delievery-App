package com.foodzie.order_fulfillment_service.data;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Field("menu_item_id")
    private String menuItemId;

    @Field("item_name")
    private String itemName;

    private Integer quantity;

    @Field("price_at_purchase")
    private BigDecimal priceAtPurchase;
}
