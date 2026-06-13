package com.foodzie.restaurant_service.controller;

import com.foodzie.restaurant_service.data.MenuItem;
import com.foodzie.restaurant_service.data.Restaurant;
import com.foodzie.restaurant_service.dto.ApiResponse;
import com.foodzie.restaurant_service.dto.MediaUploadResult;
import com.foodzie.restaurant_service.exception.MenuItemNotFoundException;
import com.foodzie.restaurant_service.exception.RestaurantNotFoundException;
import com.foodzie.restaurant_service.repository.MenuItemRepository;
import com.foodzie.restaurant_service.repository.RestaurantRepository;
import com.foodzie.restaurant_service.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Handles image upload and deletion for restaurants and menu items.
 *
 * <p>Flow for uploads:
 * <ol>
 *   <li>Receive the {@link MultipartFile} from the admin frontend.</li>
 *   <li>Forward it to Cloudinary via {@link CloudinaryService}.</li>
 *   <li>Persist the returned {@code secure_url} and {@code public_id} into MongoDB.</li>
 *   <li>Return the URLs to the frontend so it can display the new image immediately.</li>
 * </ol>
 *
 * <p>If the entity already has an image, the old Cloudinary asset is deleted first
 * to avoid orphaned files accumulating in the account.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class MediaController {

    private final CloudinaryService cloudinaryService;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    // ── Restaurant banner ─────────────────────────────────────────────────────

    /**
     * POST /api/admin/restaurants/{restaurantId}/image
     * Uploads the main banner image for a restaurant.
     * Accepts: multipart/form-data with a field named "image".
     */
    @PostMapping(
            value = "/restaurants/{restaurantId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<MediaUploadResult>> uploadRestaurantImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId,
            @RequestPart("image") MultipartFile image) throws IOException {

        String ownerEmail = jwt.getSubject();
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "Restaurant not found: " + restaurantId));

        if (!restaurant.getOwnerEmail().equals(ownerEmail)) {
            throw new AccessDeniedException("You do not own this restaurant");
        }

        // Delete the existing image from Cloudinary before uploading the new one
        if (restaurant.getImagePublicId() != null) {
            cloudinaryService.delete(restaurant.getImagePublicId());
        }

        MediaUploadResult result = cloudinaryService.upload(image, "foodzie/restaurants");

        restaurant.setImageUrl(result.getSecureUrl());
        restaurant.setImagePublicId(result.getPublicId());
        restaurantRepository.save(restaurant);

        log.info("Restaurant image updated: restaurantId={} publicId={}",
                restaurantId, result.getPublicId());

        return ResponseEntity.ok(ApiResponse.ok("Restaurant image uploaded", result));
    }

    // ── Menu item thumbnail ───────────────────────────────────────────────────

    /**
     * POST /api/admin/menu-items/{itemId}/image
     * Uploads the thumbnail for a specific menu item.
     * Accepts: multipart/form-data with a field named "image".
     */
    @PostMapping(
            value = "/menu-items/{itemId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<MediaUploadResult>> uploadMenuItemImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId,
            @RequestPart("image") MultipartFile image) throws IOException {

        String ownerEmail = jwt.getSubject();
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new MenuItemNotFoundException(
                        "Menu item not found: " + itemId));

        // Verify the caller owns the restaurant this item belongs to
        Restaurant restaurant = restaurantRepository.findById(item.getRestaurantId())
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "Restaurant not found: " + item.getRestaurantId()));

        if (!restaurant.getOwnerEmail().equals(ownerEmail)) {
            throw new AccessDeniedException("You do not own the restaurant this item belongs to");
        }

        // Delete the existing image from Cloudinary before uploading the new one
        if (item.getImagePublicId() != null) {
            cloudinaryService.delete(item.getImagePublicId());
        }

        MediaUploadResult result = cloudinaryService.upload(image, "foodzie/menu-items");

        item.setImageUrl(result.getSecureUrl());
        item.setImagePublicId(result.getPublicId());
        menuItemRepository.save(item);

        log.info("MenuItem image updated: itemId={} publicId={}", itemId, result.getPublicId());

        return ResponseEntity.ok(ApiResponse.ok("Menu item image uploaded", result));
    }

    // ── Direct deletion ───────────────────────────────────────────────────────

    /**
     * DELETE /api/admin/media/{publicId}
     * Removes an image directly from Cloudinary by its public ID.
     * Use this when deleting a restaurant or removing a menu item entirely.
     *
     * <p>The {@code publicId} path variable must be URL-encoded if it contains slashes
     * (e.g. "foodzie/restaurants/abc123" → "foodzie%2Frestaurants%2Fabc123").
     */
    @DeleteMapping("/media/{publicId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_RESTAURANT')")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable String publicId) {

        cloudinaryService.delete(publicId);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted from Cloudinary"));
    }
}
