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
 * A single food item in a restaurant's menu.
 * Stored in its own collection so it can be queried independently
 * (e.g. "all available items in category X for restaurant Y").
 */
@Document(collection = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    /** Human-readable ID, e.g. "ITEM-101". Assigned at creation. */
    @Id
    private String id;

    /** Foreign key — references restaurants.id. */
    @Indexed
    @Field("restaurant_id")
    private String restaurantId;

    private String name;

    private String description;

    /** Current price — always the live catalog price (not a snapshot). */
    private BigDecimal price;

    /** e.g. "Main Course", "Beverages", "Desserts". */
    private String category;

    @Field("image_url")
    private String imageUrl;

    /**
     * Cloudinary public_id for this image.
     * Required to delete or replace the image via the Cloudinary API.
     * Null until an image is uploaded.
     */
    @Field("image_public_id")
    private String imagePublicId;

    /**
     * When false, the item is hidden from the public menu endpoint.
     * Restaurants use this to mark items out of stock instantly.
     */
    @Builder.Default
    @Field("is_available")
    private boolean available = true;

    /**
     * Average rating for this menu item (0.0 to 5.0).
     * Calculated from user reviews; starts at 0 when no ratings exist.
     */
    @Builder.Default
    @Field("rating")
    private Double rating = 0.0;

    /**
     * Total number of ratings/reviews received for this menu item.
     * Used to compute the average rating and show rating confidence.
     */
    @Builder.Default
    @Field("total_ratings")
    private Integer totalRatings = 0;

    /**
     * Whether this menu item is vegetarian.
     * Set by restaurant admin during menu item creation/upload.
     */
    @Builder.Default
    @Field("is_veg")
    private boolean isVeg = false;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
