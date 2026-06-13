package com.foodzie.restaurant_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MenuItemResponse {
    private String id;
    private String restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private String imagePublicId;
    private boolean available;
    @JsonProperty("isVeg")
    private boolean isVeg;
    private Double rating;
    private Integer totalRatings;
    private LocalDateTime updatedAt;
}
