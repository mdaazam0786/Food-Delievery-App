package com.foodzie.user_service.service.implementation;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.foodzie.user_service.service.CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements CloudStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final String[] ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"};

    private final Cloudinary cloudinary;

    @Override
    public String upload(MultipartFile file, String folder, String publicId) {
        validateFile(file);

        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", folder);
            options.put("resource_type", "image");
            options.put("overwrite", true);
            if (publicId != null && !publicId.isBlank()) {
                options.put("public_id", publicId);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);

            String url = (String) result.get("secure_url");
            log.debug("Uploaded file to Cloudinary: {}", url);
            return url;

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Avatar upload failed. Please try again.", e);
        }
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.debug("Deleted Cloudinary asset: {}", publicId);
        } catch (IOException e) {
            // Non-fatal — log and continue; the DB record will be updated regardless
            log.warn("Failed to delete Cloudinary asset '{}': {}", publicId, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must not exceed 5 MB");
        }
        String contentType = file.getContentType();
        boolean allowed = false;
        for (String type : ALLOWED_CONTENT_TYPES) {
            if (type.equalsIgnoreCase(contentType)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        }
    }
}
