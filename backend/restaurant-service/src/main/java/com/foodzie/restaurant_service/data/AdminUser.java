package com.foodzie.restaurant_service.data;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Analytics tracking model for restaurant admins.
 * Stores business metrics like earnings and order counts.
 */
@Document(collection = "admin_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

    @Id
    private String id;

    @Indexed
    @Field("email")
    private String email;

    @Field("total_earnings")
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Field("total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Field("todays_earnings")
    @Builder.Default
    private BigDecimal todaysEarnings = BigDecimal.ZERO;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
