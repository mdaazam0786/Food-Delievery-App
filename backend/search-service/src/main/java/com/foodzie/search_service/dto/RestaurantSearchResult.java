package com.foodzie.search_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Returned to the frontend for both the home feed and text search.
 * distanceKm is computed from the Haversine formula against the stored lat/lon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantSearchResult {

    private String restaurantId;
    private String name;
    private String imageUrl;
    private String status;
    private double latitude;
    private double longitude;

    /** Distance from the user's location in kilometres. */
    private Double distanceKm;

    /** Average rating for the restaurant (0.0 to 5.0). */
    private Double rating;

    /** Total number of ratings received. */
    private Integer totalRatings;

    /** Promotional discount string, e.g. "20% OFF". */
    private String discount;

    /** Lightweight menu item list — name + category only (no nested document needed). */
    private List<MenuItemResult> menuItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuItemResult {
        private String name;
        private String category;
        private Boolean isVeg;
        private Double rating;
        private Integer totalRatings;
    }
}
