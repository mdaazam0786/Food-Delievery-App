package com.foodzie.search_service.document;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

/**
 * MongoDB document mapped to the "restaurants" collection.
 *
 * Matches the schema written by restaurant-service's RestaurantServiceImpl#writeRestaurant()
 * exactly — includes nested menu items for cross-field searching via text indexes.
 *
 * restaurant-service writes to this collection whenever a restaurant or menu item changes.
 * search-service reads from it and uses text indexes for full-text search.
 */
@Document(collection = "restaurants")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestaurantDocument {

    @Id
    private String id;

    @TextIndexed
    private String name;

    @TextIndexed
    private String description;

    @Indexed
    private String status;

    private String ownerEmail;

    @TextIndexed
    private String addressText;

    /** Raw latitude — used for geospatial queries. */
    private double latitude;

    /** Raw longitude — used for geospatial queries. */
    private double longitude;

    private String imageUrl;

    /** Average rating for the restaurant (0.0 to 5.0). */
    private Double rating;

    /** Total number of ratings received. */
    private Integer totalRatings;

    /** Promotional discount string, e.g. "20% OFF". */
    private String discount;

    /**
     * Nested menu items with full details for cross-field searching.
     * Allows searching on menu item names and descriptions.
     * Defaults to empty list.
     */
    @Builder.Default
    private List<MenuItemIndexDoc> menuItems = new java.util.ArrayList<>();

    /**
     * Nested menu item document for MongoDB indexing.
     * Includes all relevant fields for search and faceting.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MenuItemIndexDoc {
        private String id;

        @TextIndexed
        private String name;

        @TextIndexed
        private String description;

        private String category;

        private Double price;

        private boolean available;

        @JsonProperty("isVeg")
        private boolean isVeg;

        private Double rating;

        private Integer totalRatings;
    }
}
