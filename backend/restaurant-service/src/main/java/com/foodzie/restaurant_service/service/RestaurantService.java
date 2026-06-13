package com.foodzie.restaurant_service.service;

import com.foodzie.restaurant_service.dto.*;

import java.util.List;

public interface RestaurantService {

    // ── Management (owner/admin) ──────────────────────────────────────────────

    RestaurantResponse createRestaurant(String ownerEmail, CreateRestaurantRequest request);

    RestaurantResponse getMyRestaurant(String ownerEmail);

    RestaurantResponse updateStatus(String restaurantId, String ownerEmail, UpdateRestaurantStatusRequest request);

    RestaurantResponse updateRating(String restaurantId, String ownerEmail, UpdateRestaurantRatingRequest request);

    MenuItemResponse addMenuItem(String restaurantId, String ownerEmail, AddMenuItemRequest request);

    MenuItemResponse updateMenuItem(String restaurantId, String itemId, String ownerEmail, UpdateMenuItemRequest request);

    // ── Public (app users / other services) ──────────────────────────────────

    RestaurantResponse getRestaurant(String restaurantId);

    List<MenuItemResponse> getPublicMenu(String restaurantId);

    // ── Media management ──────────────────────────────────────────────────────

    void reindexRestaurant(String restaurantId);

    MenuItemResponse uploadMenuItemImage(String itemId, String ownerEmail, String imageUrl, String imagePublicId);
}
