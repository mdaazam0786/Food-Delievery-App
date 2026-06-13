package com.foodzie.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update a restaurant's rating and discount.
 * Used by admin or rating service to update these promotional/review fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRestaurantRatingRequest {

    /** Average rating (0.0 to 5.0). */
    private Double rating;

    /** Total number of ratings received. */
    private Integer totalRatings;

    /** Promotional discount string, e.g. "20% OFF". */
    private String discount;
}
