package com.foodzie.search_service.controller;

import com.foodzie.search_service.dto.ApiResponse;
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
    ) throws IOException {

        log.debug("Nearby request: lat={} lon={} radius={}km page={} size={}",
                lat, lon, radius, page, size);

        PagedSearchResponse response = searchService.nearby(lat, lon, radius, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Nearby restaurants", response));
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
    ) throws IOException {

        log.debug("Search request: q='{}' lat={} lon={} radius={}km page={} size={}",
                q, lat, lon, radius, page, size);

        PagedSearchResponse response = searchService.search(q, lat, lon, radius, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
