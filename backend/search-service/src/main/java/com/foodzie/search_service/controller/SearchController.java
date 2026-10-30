package com.foodzie.search_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
import com.foodzie.search_service.dto.PagedSearchResponse;
import com.foodzie.search_service.service.RestaurantSearchService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Public search endpoints consumed by the React Native app.
 *
 * ── GET /api/search/nearby ────────────────────────────────────────────────────
 * Home feed — OPEN restaurants within a radius, sorted by distance.
 * Called when the user opens the app and location permission is granted.
 *
 * ── GET /api/search ───────────────────────────────────────────────────────────
 * Universal search — fuzzy text match on restaurant name and menu item names,
 * with Gaussian geo-decay scoring so nearby relevant results rank higher.
 * Called when the user types in the search bar.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final RestaurantSearchService searchService;

    /**
     * GET /api/search/nearby
     *
     * Query params:
     *   lat    — user latitude  (required, -90 to 90)
     *   lon    — user longitude (required, -180 to 180)
     *   radius — search radius in km (optional, default 5, max 50)
     *   page   — 0-indexed page number (optional, default 0)
     *   size   — results per page (optional, default 20, max 100)
     *
     * Returns OPEN restaurants within the radius sorted by distance ascending.
     * The closest restaurant always appears first.
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<PagedSearchResponse>> nearby(
            @RequestParam @NotNull
            @DecimalMin("-90.0") @DecimalMax("90.0")   Double lat,
            @RequestParam @NotNull
            @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(defaultValue = "5.0")
            @DecimalMin("0.1") @DecimalMax("50.0")     double radius,
            @RequestParam(defaultValue = "0")  @Min(0)         int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        try {
            log.debug("Nearby request: lat={} lon={} radius={}km page={} size={}",
                    lat, lon, radius, page, size);
            
            // Validate input
            if (lat == null || lon == null) {
                log.warn("Latitude or longitude is null");
                throw new BadRequestException("Latitude and longitude are required");
            }
            if (lat < -90.0 || lat > 90.0) {
                log.warn("Invalid latitude: {}", lat);
                throw BadRequestException.of("lat", "Latitude must be between -90 and 90");
            }
            if (lon < -180.0 || lon > 180.0) {
                log.warn("Invalid longitude: {}", lon);
                throw BadRequestException.of("lon", "Longitude must be between -180 and 180");
            }
            if (radius <= 0 || radius > 50) {
                log.warn("Invalid radius: {}", radius);
                throw BadRequestException.of("radius", "Radius must be between 0.1 and 50 km");
            }
            if (page < 0) {
                log.warn("Invalid page number: {}", page);
                throw BadRequestException.of("page", "Page number cannot be negative");
            }
            if (size < 1 || size > 100) {
                log.warn("Invalid page size: {}", size);
                throw BadRequestException.of("size", "Page size must be between 1 and 100");
            }

            PagedSearchResponse response = searchService.nearby(lat, lon, radius, page, size);
            log.debug("Nearby search completed successfully");
            return ResponseEntity.ok(ApiResponse.ok("Nearby restaurants", response));
            
        } catch (BadRequestException e) {
            log.warn("Bad request in nearby search: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("IO error during nearby search", e);
            throw new InternalServerException("Failed to search nearby restaurants", e);
        } catch (Exception e) {
            log.error("Unexpected error in nearby search", e);
            throw new InternalServerException("Failed to search nearby restaurants", e);
        }
    }

    /**
     * GET /api/search?q=Biryani&lat=28.7041&lon=77.1025
     *
     * Query params:
     *   q      — search term (required)
     *   lat    — user latitude  (required)
     *   lon    — user longitude (required)
     *   radius — search radius in km (optional, default 10, max 50)
     *   page   — 0-indexed page number (optional, default 0)
     *   size   — results per page (optional, default 20, max 100)
     *
     * Fuzzy text search with Gaussian geo-decay scoring.
     * Results are ranked by a blend of text relevance and proximity —
     * a highly relevant restaurant 1km away ranks above a perfect match 9km away.
     *
     * Typo correction is automatic: "Piza" → "Pizza", "Biryni" → "Biryani".
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedSearchResponse>> search(
            @RequestParam                                          String q,
            @RequestParam @NotNull
            @DecimalMin("-90.0") @DecimalMax("90.0")              Double lat,
            @RequestParam @NotNull
            @DecimalMin("-180.0") @DecimalMax("180.0")            Double lon,
            @RequestParam(defaultValue = "10.0")
            @DecimalMin("0.1") @DecimalMax("50.0")                double radius,
            @RequestParam(defaultValue = "0")  @Min(0)            int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100)  int size
    ) {
        try {
            log.debug("Search request: q='{}' lat={} lon={} radius={}km page={} size={}",
                    q, lat, lon, radius, page, size);
            
            // Validate input
            if (q == null || q.isBlank()) {
                log.warn("Search query is null or blank");
                throw BadRequestException.of("q", "Search query is required");
            }
            if (lat == null || lon == null) {
                log.warn("Latitude or longitude is null");
                throw new BadRequestException("Latitude and longitude are required");
            }
            if (lat < -90.0 || lat > 90.0) {
                log.warn("Invalid latitude: {}", lat);
                throw BadRequestException.of("lat", "Latitude must be between -90 and 90");
            }
            if (lon < -180.0 || lon > 180.0) {
                log.warn("Invalid longitude: {}", lon);
                throw BadRequestException.of("lon", "Longitude must be between -180 and 180");
            }
            if (radius <= 0 || radius > 50) {
                log.warn("Invalid radius: {}", radius);
                throw BadRequestException.of("radius", "Radius must be between 0.1 and 50 km");
            }
            if (page < 0) {
                log.warn("Invalid page number: {}", page);
                throw BadRequestException.of("page", "Page number cannot be negative");
            }
            if (size < 1 || size > 100) {
                log.warn("Invalid page size: {}", size);
                throw BadRequestException.of("size", "Page size must be between 1 and 100");
            }

            PagedSearchResponse response = searchService.search(q, lat, lon, radius, page, size);
            log.debug("Search completed successfully");
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (BadRequestException e) {
            log.warn("Bad request in search: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("IO error during search", e);
            throw new InternalServerException("Failed to search restaurants", e);
        } catch (Exception e) {
            log.error("Unexpected error in search", e);
            throw new InternalServerException("Failed to search restaurants", e);
        }
    }
}
