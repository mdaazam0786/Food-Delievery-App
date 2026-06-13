package com.foodzie.order_service.data;

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

@Document(collection = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private String id;

    /** Extracted from the JWT forwarded by the API Gateway. */
    @Indexed
    @Field("user_email")
    private String userEmail;

    /** References the restaurant in the catalog service. */
    @Field("restaurant_id")
    private String restaurantId;

    /** References the address saved in user-service. */
    @Field("delivery_address_id")
    private Long deliveryAddressId;

    /**
     * Assigned delivery partner ID — set when delivery-acceptance-service
     * publishes a DeliveryPartnerAssignedEvent. Null until a driver is assigned.
     */
    @Field("driver_id")
    private String driverId;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Field("total_amount")
    private BigDecimal totalAmount;

    @Builder.Default
    private String currency = "INR";

    /** Order items embedded directly — no separate collection needed. */
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /** Subtotal of all food items (sum of quantity * price). */
    @Field("food_subtotal")
    private BigDecimal foodSubtotal;

    /** Delivery fee calculated from distance and delivery rate. */
    @Field("delivery_fee")
    private BigDecimal deliveryFee;

    /** GST amount (18% of food subtotal). */
    @Field("gst_amount")
    private BigDecimal gstAmount;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
