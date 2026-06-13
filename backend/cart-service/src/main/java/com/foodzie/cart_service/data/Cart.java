package com.foodzie.cart_service.data;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    private String id;

    /**
     * One cart per user — unique index enforced at DB level.
     * Extracted from the JWT subject (email).
     */
    @Indexed(unique = true)
    @Field("user_email")
    private String userEmail;

    /**
     * The restaurant this cart belongs to.
     * A user can only have items from ONE restaurant at a time.
     * Changing restaurant clears the cart.
     */
    @Field("restaurant_id")
    private String restaurantId;

    /** Cart items embedded directly — no separate collection needed. */
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
