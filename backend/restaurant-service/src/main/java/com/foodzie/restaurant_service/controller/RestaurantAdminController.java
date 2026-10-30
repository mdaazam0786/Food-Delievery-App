package com.foodzie.restaurant_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            String ownerEmail = jwt.getSubject();
            log.debug("Fetching restaurant for owner: {}", ownerEmail);
            return ResponseEntity.ok(ApiResponse.ok(
                    restaurantService.getMyRestaurant(ownerEmail)));
        } catch (UnauthorizedException ex) {
            log.warn("Unauthorized access: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching restaurant for authenticated user: {}", ex.getMessage(), ex);
            throw ex;
        }
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            String ownerEmail = jwt.getSubject();
            log.info("Creating restaurant for owner: {}", ownerEmail);
            RestaurantResponse response = restaurantService.createRestaurant(ownerEmail, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Restaurant onboarded successfully", response));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (ConflictException ex) {
            log.warn("Conflict creating restaurant: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating restaurant: {}", ex.getMessage(), ex);
            throw ex;
        }
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new BadRequestException("Restaurant ID cannot be empty");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            String ownerEmail = jwt.getSubject();
            log.info("Updating status for restaurant: {} by user: {}", restaurantId, ownerEmail);
            return ResponseEntity.ok(ApiResponse.ok("Status updated",
                    restaurantService.updateStatus(restaurantId, ownerEmail, request)));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating restaurant status: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * GET /api/admin/restaurants/{restaurantId}/menu
     * Fetches paginated menu items for the owner's restaurant.
     * Verifies that the caller owns the restaurant.
     * 
     * Query params: ?page=0&size=20&sort=id,desc
     */
    @GetMapping("/{restaurantId}/menu")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<MenuItemResponse>>> getMenuItemsByOwner(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId,
            org.springframework.data.domain.Pageable pageable) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new BadRequestException("Restaurant ID cannot be empty");
            }
            String ownerEmail = jwt.getSubject();
            log.debug("Fetching menu items for restaurant: {} by owner: {}", restaurantId, ownerEmail);
            org.springframework.data.domain.Page<MenuItemResponse> response = 
                    restaurantService.getMenuItemsByOwner(restaurantId, ownerEmail, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error fetching menu items for restaurant {}: {}", restaurantId, ex.getMessage(), ex);
            throw ex;
        }
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new BadRequestException("Restaurant ID cannot be empty");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            String ownerEmail = jwt.getSubject();
            log.info("Adding menu item to restaurant: {} by owner: {}", restaurantId, ownerEmail);
            MenuItemResponse response = restaurantService.addMenuItem(restaurantId, ownerEmail, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Menu item added", response));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error adding menu item to restaurant {}: {}", restaurantId, ex.getMessage(), ex);
            throw ex;
        }
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new BadRequestException("Restaurant ID cannot be empty");
            }
            if (itemId == null || itemId.isBlank()) {
                throw new BadRequestException("Item ID cannot be empty");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            String ownerEmail = jwt.getSubject();
            log.info("Updating menu item: {} for restaurant: {} by owner: {}", itemId, restaurantId, ownerEmail);
            return ResponseEntity.ok(ApiResponse.ok("Menu item updated",
                    restaurantService.updateMenuItem(restaurantId, itemId, ownerEmail, request)));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating menu item {}: {}", itemId, ex.getMessage(), ex);
            throw ex;
        }
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
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                log.warn("No file provided for restaurant image upload");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No file provided"));
            }

            log.info("Uploading restaurant image, size: {} bytes", file.getSize());
            MediaUploadResult result = cloudinaryService.upload(file, "foodzie/restaurants");
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", result.getSecureUrl());
            log.info("Restaurant image uploaded successfully: {}", result.getSecureUrl());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid image file: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("IO error during restaurant image upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process image file"));
        } catch (Exception e) {
            log.error("Image upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Image upload failed: " + e.getMessage()));
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
            @RequestParam("file") MultipartFile file) {
        try {
            if (jwt == null || jwt.getSubject() == null) {
                log.warn("Unauthorized access to upload menu item image");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }
            if (itemId == null || itemId.isBlank()) {
                log.warn("Item ID is empty for menu item image upload");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Item ID cannot be empty"));
            }
            if (file == null || file.isEmpty()) {
                log.warn("No file provided for menu item image upload");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No file provided"));
            }

            String ownerEmail = jwt.getSubject();
            log.info("Uploading menu item image for item: {} by user: {}", itemId, ownerEmail);
            
            MediaUploadResult result = cloudinaryService.upload(file, "foodzie/menu-items");
            restaurantService.uploadMenuItemImage(itemId, ownerEmail, result.getSecureUrl(), result.getPublicId());
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", result.getSecureUrl());
            log.info("Menu item image uploaded successfully: {}", result.getSecureUrl());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid image file: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("IO error during menu item image upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process image file"));
        } catch (Exception e) {
            log.error("Menu item image upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Image upload failed: " + e.getMessage()));
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
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new UnauthorizedException("Authentication token is missing or invalid");
            }
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new BadRequestException("Restaurant ID cannot be empty");
            }
            if (request == null) {
                throw new BadRequestException("Request body cannot be empty");
            }
            String ownerEmail = jwt.getSubject();
            log.info("Updating rating for restaurant: {} by owner: {}", restaurantId, ownerEmail);
            return ResponseEntity.ok(ApiResponse.ok("Rating updated",
                    restaurantService.updateRating(restaurantId, ownerEmail, request)));
        } catch (BadRequestException | UnauthorizedException ex) {
            log.warn("Invalid request: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error updating restaurant rating: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
