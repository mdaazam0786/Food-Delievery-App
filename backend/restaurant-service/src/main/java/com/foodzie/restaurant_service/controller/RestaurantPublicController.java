package com.foodzie.restaurant_service.controller;

import com.foodzie.restaurant_service.dto.ApiResponse;
import com.foodzie.restaurant_service.dto.MenuItemResponse;
import com.foodzie.restaurant_service.dto.RestaurantResponse;
import com.foodzie.restaurant_service.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public read-only endpoints — accessible to any authenticated user.
 * These are the endpoints the React Native app calls to render the restaurant page.
 */
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantPublicController {

    private final RestaurantService restaurantService;

    /**
     * GET /api/restaurants/{restaurantId}
     * Returns core metadata for a single restaurant (name, address, status, image).
     */
    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant(
            @PathVariable String restaurantId) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.getRestaurant(restaurantId)));
    }

    /**
     * GET /api/restaurants/{restaurantId}/menu
     * Returns only available menu items for the restaurant.
     * Unavailable items (out of stock) are filtered out automatically.
     */
    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getMenu(
            @PathVariable String restaurantId) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.getPublicMenu(restaurantId)));
    }
}
