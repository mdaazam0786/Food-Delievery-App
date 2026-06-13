package com.foodzie.restaurant_service.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Wires the Cloudinary SDK client as a Spring bean.
 * Credentials are injected from environment variables — never hardcoded.
 *
 * <p>Required env vars:
 * <ul>
 *   <li>{@code CLOUDINARY_CLOUD_NAME}</li>
 *   <li>{@code CLOUDINARY_API_KEY}</li>
 *   <li>{@code CLOUDINARY_API_SECRET}</li>
 * </ul>
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true          // always use HTTPS URLs
        ));
    }
}
