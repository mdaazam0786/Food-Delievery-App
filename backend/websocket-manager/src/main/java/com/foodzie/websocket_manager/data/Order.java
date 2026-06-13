package com.foodzie.websocket_manager.data;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Minimal projection of the orders collection.
 * This service only needs userEmail and driverId — no items, totals, etc.
 */
@Document(collection = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private String id;

    @Field("user_email")
    private String userEmail;

    @Field("driver_id")
    private String driverId;

    private String status;
}
