package com.foodzie.restaurant_service.data;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

/**
 * MongoDB document for restaurant data with text search support.
 * 
 * This is the primary data model for restaurants in MongoDB.
 * Text indexes on name and description enable full-text search.
 */
@Document(collection = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RestaurantDocument {

    @Id
    private String id;

    @TextIndexed
    private String name;

    @TextIndexed
    private String description;

    private String status;

    private String ownerEmail;

    private String addressText;

    private double latitude;

    private double longitude;

    private String imageUrl;

    private Double rating;

    private Integer totalRatings;

    /** Promotional discount string, e.g. "20% OFF" or "Free Delivery". */
    private String discount;

    /**
     * Embedded menu items for restaurant.
     * Defaults to empty list.
     */
    @Builder.Default
    private List<MenuItemIndexDoc> menuItems = new java.util.ArrayList<>();

    /**
     * Nested menu item document for MongoDB.
     * Includes all relevant fields for search and display.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MenuItemIndexDoc {
        private String name;

        private String description;

        private String category;

        private Double price;

        private boolean available;

        private boolean isVeg;

        private Double rating;

        private Integer totalRatings;

        private String imageURL;

        private String imagePublicId;

        private java.time.LocalDateTime updatedAt;
    }
}
