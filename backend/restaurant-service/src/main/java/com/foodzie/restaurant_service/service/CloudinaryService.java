package com.foodzie.restaurant_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.foodzie.restaurant_service.dto.MediaUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Handles all communication with the Cloudinary API.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Upload a {@link MultipartFile} and return the {@code secure_url} and {@code public_id}.</li>
 *   <li>Delete an image by its {@code public_id} (used when replacing or removing images).</li>
 * </ul>
 *
 * <p>This class is intentionally kept thin — it does not touch the database.
 * Callers ({@link MediaController}) are responsible for persisting the returned URLs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a file to Cloudinary under the given folder.
     *
     * @param file   the multipart image file from the HTTP request
     * @param folder Cloudinary folder path, e.g. "foodzie/restaurants" or "foodzie/menu-items"
     * @return a {@link MediaUploadResult} containing the secure URL and public ID
     * @throws IOException if the upload fails or the file cannot be read
     */
    @SuppressWarnings("unchecked")
    public MediaUploadResult upload(MultipartFile file, String folder) throws IOException {
        validateImageFile(file);

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder",          folder,
                        "resource_type",   "image",
                        // Auto-format and quality for optimal delivery
                        "format",          "webp",
                        "quality",         "auto",
                        // Eager transformation: resize to a max 1200px wide thumbnail
                        "eager",           List.of(
                                new Transformation()
                                        .crop("limit")
                                        .width(1200)
                                        .height(800)
                        )
                )
        );

        String secureUrl = (String) uploadResult.get("secure_url");
        String publicId  = (String) uploadResult.get("public_id");

        log.info("Cloudinary upload success: publicId={} url={}", publicId, secureUrl);
        return new MediaUploadResult(secureUrl, publicId);
    }


    /**
     * Deletes an image from Cloudinary by its public ID.
     * Safe to call even if the image no longer exists — Cloudinary returns "not found"
     * which we log and swallow rather than propagating as an error.
     *
     * @param publicId the Cloudinary public_id stored in the database
     */
    @SuppressWarnings("unchecked")
    public void delete(String publicId) {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId, ObjectUtils.asMap("resource_type", "image")
            );
            String outcome = (String) result.get("result");
            if ("ok".equals(outcome)) {
                log.info("Cloudinary delete success: publicId={}", publicId);
            } else {
                log.warn("Cloudinary delete returned '{}' for publicId={}", outcome, publicId);
            }
        } catch (IOException e) {
            // Deletion failure must not break the caller's flow.
            // The image may already be gone or the public_id may be stale.
            log.error("Cloudinary delete failed for publicId={}: {}", publicId, e.getMessage());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Invalid file type: " + contentType + ". Only image files are accepted.");
        }

        // 10 MB hard limit (also enforced at the multipart resolver level)
        long maxBytes = 10L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds the 10 MB limit");
        }
    }
}
