package com.foodzie.restaurant_service.dto;

import com.foodzie.restaurant_service.data.RestaurantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RestaurantResponse {
    private String id;
    private String ownerEmail;
    private String name;
    private String description;
    private String addressText;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private String imagePublicId;
    private String gstNo;
    private String fssaiNo;
    private RestaurantStatus status;
    private Double rating;
    private Integer totalRatings;
    private String discount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MenuItemResponse> menuItems;
}
