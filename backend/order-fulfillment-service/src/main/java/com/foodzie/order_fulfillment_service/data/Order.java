package com.foodzie.order_fulfillment_service.data;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read/write mirror of the orders collection owned by order-service.
 * This service only updates the status field and reads userEmail, restaurantId,
 * driverId, and totalAmount for event payloads.
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

    @Indexed
    @Field("user_email")
    private String userEmail;

    @Field("restaurant_id")
    private String restaurantId;

    @Field("delivery_address_id")
    private Long deliveryAddressId;

    /** Set by order-service when DeliveryPartnerAssignedEvent is consumed. */
    @Field("driver_id")
    private String driverId;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Field("total_amount")
    private BigDecimal totalAmount;

    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
