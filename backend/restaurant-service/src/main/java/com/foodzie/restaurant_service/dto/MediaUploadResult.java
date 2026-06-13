package com.foodzie.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by {@link com.foodzie.restaurant_service.service.CloudinaryService#upload}
 * and also sent back to the frontend after a successful image upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResult {

    /** HTTPS URL to display the image in the app. Stored in image_url. */
    private String secureUrl;

    /**
     * Cloudinary's unique identifier for this asset.
     * Stored in image_public_id — required to delete or replace the image later.
     */
    private String publicId;
}
