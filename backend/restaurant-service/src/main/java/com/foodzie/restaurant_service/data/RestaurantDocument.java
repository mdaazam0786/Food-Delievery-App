package com.foodzie.restaurant_service.data;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Elasticsearch index document for restaurant search.
 *
 * <p>This is a denormalized read-model kept in sync with the MongoDB source of truth.
 * It is written whenever a restaurant is created/updated or a menu item changes.
 * The search-service (or this service's public search endpoint) queries this index.
 *
 * <p>Index name: "restaurants"
 */
@Document(indexName = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RestaurantDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String ownerEmail;

    @Field(type = FieldType.Text)
    private String addressText;

    @Field(type = FieldType.Double)
    private double latitude;

    @Field(type = FieldType.Double)
    private double longitude;

    @Field(type = FieldType.Text)
    private String imageUrl;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Integer)
    private Integer totalRatings;

    /** Promotional discount string, e.g. "20% OFF" or "Free Delivery". */
    @Field(type = FieldType.Keyword)
    private String discount;

    /**
     * Nested menu items with full details for cross-field searching.
     * Allows searching on menu item names and descriptions.
     * Defaults to empty list.
     */
    @Field(type = FieldType.Nested, includeInParent = true)
    @Builder.Default
    private List<MenuItemIndexDoc> menuItems = new java.util.ArrayList<>();

    /**
     * Nested menu item document for Elasticsearch indexing.
     * Includes all relevant fields for search and faceting.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MenuItemIndexDoc {
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String name;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String description;

        @Field(type = FieldType.Keyword)
        private String category;

        @Field(type = FieldType.Double)
        private Double price;

        @Field(type = FieldType.Boolean)
        private boolean available;

        @Field(type = FieldType.Boolean)
        private boolean isVeg;

        @Field(type = FieldType.Double)
        private Double rating;

        @Field(type = FieldType.Integer)
        private Integer totalRatings;
    }
}
