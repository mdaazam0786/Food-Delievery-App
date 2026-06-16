package com.foodzie.restaurant_service.controller;

import com.foodzie.restaurant_service.data.MenuItem;
import com.foodzie.restaurant_service.data.Restaurant;
import com.foodzie.restaurant_service.dto.*;
import com.foodzie.restaurant_service.exception.MenuItemNotFoundException;
import com.foodzie.restaurant_service.exception.RestaurantNotFoundException;
import com.foodzie.restaurant_service.service.RestaurantService;
import com.foodzie.restaurant_service.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Management endpoints — restricted to restaurant owners and admins.
 *
 * <p>All write operations extract the caller's email from the JWT subject
 * and verify ownership before mutating any data.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/restaurants")
@RequiredArgsConstructor
public class RestaurantAdminController {

    private final RestaurantService restaurantService;
    private final CloudinaryService cloudinaryService;

    /**
     * GET /api/admin/restaurants/mine
     * Returns the restaurant owned by the authenticated caller.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getMyRestaurant(
            @AuthenticationPrincipal Jwt jwt) {
        String ownerEmail = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok(
                restaurantService.getMyRestaurant(ownerEmail)));
    }

    /**
     * POST /api/admin/restaurants
     * Provisions a new restaurant or completes an auto-provisioned stub.
     * The JWT subject becomes the owner_email.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRestaurantRequest request) {
        System.out.println("ENTERED CONTROLLER");
        String ownerEmail = jwt.getSubject();
        RestaurantResponse response = restaurantService.createRestaurant(ownerEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Restaurant onboarded successfully", response));
    }

    /**
     * PUT /api/admin/restaurants/{restaurantId}/status
     * Opens or closes the restaurant. Used by the owner's dashboard.
     */
    @PutMapping("/{restaurantId}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId,
            @Valid @RequestBody UpdateRestaurantStatusRequest request) {
        String ownerEmail = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok("Status updated",
                restaurantService.updateStatus(restaurantId, ownerEmail, request)));
    }

    /**
     * GET /api/admin/restaurants/{restaurantId}/menu
     * Fetches all menu items for the owner's restaurant.
     * Verifies that the caller owns the restaurant.
     */
    @GetMapping("/{restaurantId}/menu")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<java.util.List<MenuItemResponse>>> getMenuItemsByOwner(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId) {
        String ownerEmail = jwt.getSubject();
        java.util.List<MenuItemResponse> items = restaurantService.getMenuItemsByOwner(restaurantId, ownerEmail);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    /**
     * POST /api/admin/restaurants/{restaurantId}/menu
     * Adds a new item to the restaurant's menu.
     */
    @PostMapping("/{restaurantId}/menu")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId,
            @Valid @RequestBody AddMenuItemRequest request) {
        String ownerEmail = jwt.getSubject();
        MenuItemResponse response = restaurantService.addMenuItem(restaurantId, ownerEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Menu item added", response));
    }

    /**
     * PATCH /api/admin/restaurants/{restaurantId}/menu/{itemId}
     * Updates a specific menu item. Supports partial updates (patch semantics).
     * Use {@code "available": false} to instantly mark an item out of stock.
     */
    @PatchMapping("/{restaurantId}/menu/{itemId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        String ownerEmail = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok("Menu item updated",
                restaurantService.updateMenuItem(restaurantId, itemId, ownerEmail, request)));
    }

    /**
     * POST /api/admin/restaurants/upload-image
     * 
     * Dedicated endpoint for uploading restaurant images during provisioning.
     * Triggered immediately when the admin selects a file.
     * This endpoint does NOT update the database - only returns the uploaded URL.
     * 
     * Request: multipart/form-data with file parameter "file"
     * Response: { "imageUrl": "https://..." } — the secure URL from Cloudinary
     * 
     * @param file the image file from the form input
     * @return 200 OK with JSON map containing the imageUrl string
     * @throws IOException if the upload fails
     */
    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<Map<String, String>> uploadImageProvisioning(
            @RequestParam("file") MultipartFile file) throws IOException {
        try {
            if (file == null || file.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "No file provided");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            MediaUploadResult result = cloudinaryService.upload(file, "foodzie/restaurants");
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", result.getSecureUrl());
            log.info("Restaurant image uploaded during provisioning: {}", result.getSecureUrl());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid image file: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (ClassCastException e) {
            log.error("Deserialization error during image upload (Elasticsearch type mismatch): {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal error during upload. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            log.error("Image upload failed", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Image upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * POST /api/admin/menu-items/{itemId}/upload-image
     * Uploads a menu item image to Cloudinary and saves the URL to MongoDB.
     */
    @PostMapping("/menu-items/{itemId}/upload-image")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<Map<String, String>> uploadMenuItemImage(
            @PathVariable String itemId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) throws IOException {
        try {
            String ownerEmail = jwt.getSubject();
            
            // Upload file to Cloudinary
            MediaUploadResult result = cloudinaryService.upload(file, "foodzie/menu-items");
            
            // Update menu item in service layer
            restaurantService.uploadMenuItemImage(itemId, ownerEmail, result.getSecureUrl(), result.getPublicId());
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", result.getSecureUrl());
            log.info("Menu item image uploaded: {}", result.getSecureUrl());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid image file: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Image upload failed", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Image upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * PATCH /api/admin/restaurants/{restaurantId}/rating
     * Updates restaurant rating, total ratings, and discount.
     */
    @PatchMapping("/{restaurantId}/rating")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRating(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId,
            @Valid @RequestBody UpdateRestaurantRatingRequest request) {
        String ownerEmail = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok("Rating updated",
                restaurantService.updateRating(restaurantId, ownerEmail, request)));
    }
}
