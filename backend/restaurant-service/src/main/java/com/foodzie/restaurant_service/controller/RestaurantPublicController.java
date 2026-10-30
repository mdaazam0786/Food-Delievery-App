package com.foodzie.restaurant_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.restaurant_service.dto.MenuItemResponse;
import com.foodzie.restaurant_service.dto.RestaurantResponse;
import com.foodzie.restaurant_service.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public read-only endpoints — accessible to any authenticated user.
 * These are the endpoints the React Native app calls to render the restaurant page.
 */
@Slf4j
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
        try {
            log.debug("Fetching restaurant: {}", restaurantId);
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new IllegalArgumentException("Restaurant ID cannot be empty");
            }
            return ResponseEntity.ok(ApiResponse.ok(restaurantService.getRestaurant(restaurantId)));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid restaurant ID: {}", restaurantId);
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching restaurant {}: {}", restaurantId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * GET /api/restaurants/{restaurantId}/menu
     * Returns only available menu items for the restaurant.
     * Unavailable items (out of stock) are filtered out automatically.
     */
    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getMenu(
            @PathVariable String restaurantId) {
        try {
            log.debug("Fetching menu for restaurant: {}", restaurantId);
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new IllegalArgumentException("Restaurant ID cannot be empty");
            }
            return ResponseEntity.ok(ApiResponse.ok(restaurantService.getPublicMenu(restaurantId)));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid restaurant ID: {}", restaurantId);
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching menu for restaurant {}: {}", restaurantId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
