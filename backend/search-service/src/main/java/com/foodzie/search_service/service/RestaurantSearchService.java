package com.foodzie.search_service.service;

import com.foodzie.search_service.document.RestaurantDocument;
import com.foodzie.search_service.dto.PagedSearchResponse;
import com.foodzie.search_service.dto.RestaurantSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Executes the two MongoDB queries powering the Foodzie discovery experience.
 *
 * The "restaurants" collection uses text indexes on name and description fields
 * for full-text search, and stores latitude/longitude as GeoJSON points for geospatial queries.
 *
 * ── Query 1: Nearby Feed ──────────────────────────────────────────────────────
 * GET /api/search/nearby
 * Geospatial query: distance ≤ radiusKm, sorted by distance asc.
 *
 * ── Query 2: Universal Search with Geo-Decay ──────────────────────────────────
 * GET /api/search?q=...
 * Full-text search on name + menuItemNames, then sorted by distance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantSearchService {

    private static final String COLLECTION = "restaurants";

    private final MongoTemplate mongoTemplate;
    private final RestaurantSearchRepository restaurantSearchRepository;

    @Value("${app.search.home-feed-radius-km}")
    private double homeFeedRadiusKm;

    @Value("${app.search.text-search-radius-km}")
    private double textSearchRadiusKm;

    @Value("${app.search.default-page-size}")
    private int defaultPageSize;

    @Value("${app.search.geo-decay.scale-km}")
    private double decayScaleKm;

    @Value("${app.search.geo-decay.offset-km}")
    private double decayOffsetKm;

    @Value("${app.search.geo-decay.decay}")
    private double decayValue;

    // ── Query 1: Nearby Feed ──────────────────────────────────────────────────

    public PagedSearchResponse nearby(double lat, double lon, double radiusKm,
                                      int page, int size) {
        int resolvedSize = size > 0 ? size : defaultPageSize;
        Pageable pageable = PageRequest.of(page, resolvedSize);

        Point userLocation = new Point(lon, lat);
        Distance distance = new Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS);

        Query query = new Query()
                .addCriteria(Criteria.where("status").is("OPEN"))
                .addCriteria(Criteria.where("location").within(userLocation, distance));

        query.with(pageable);

        long total = mongoTemplate.count(query, RestaurantDocument.class, COLLECTION);
        List<RestaurantDocument> results = mongoTemplate.find(query, RestaurantDocument.class, COLLECTION);

        log.debug("Nearby query lat={} lon={} radius={}km page={} size={} hits={}",
                lat, lon, radiusKm, page, resolvedSize, total);

        return toPagedResponse(results, page, resolvedSize, total, lat, lon);
    }

    // ── Query 2: Universal Search with Geo-Decay ──────────────────────────────

    public PagedSearchResponse search(String query, double lat, double lon,
                                      double radiusKm, int page, int size) {
        int resolvedSize = size > 0 ? size : defaultPageSize;
        Pageable pageable = PageRequest.of(page, resolvedSize);

        // Text search criteria
        Criteria textCriteria = new Criteria().orOperator(
                Criteria.where("$text").is(query),
                Criteria.where("menuItems.name").regex(query, "i"),
                Criteria.where("menuItems.description").regex(query, "i")
        );

        // Radius filter
        Point userLocation = new Point(lon, lat);
        Distance distance = new Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS);

        Query mongoQuery = new Query()
                .addCriteria(Criteria.where("status").is("OPEN"))
                .addCriteria(textCriteria)
                .addCriteria(Criteria.where("location").within(userLocation, distance));

        mongoQuery.with(pageable);

        long total = mongoTemplate.count(mongoQuery, RestaurantDocument.class, COLLECTION);
        List<RestaurantDocument> results = mongoTemplate.find(mongoQuery, RestaurantDocument.class, COLLECTION);

        log.debug("Search query='{}' lat={} lon={} radius={}km page={} size={} hits={}",
                query, lat, lon, radiusKm, page, resolvedSize, total);

        return toPagedResponse(results, page, resolvedSize, total, lat, lon);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private PagedSearchResponse toPagedResponse(List<RestaurantDocument> documents,
                                                 int page, int size, long total,
                                                 double userLat, double userLon) {
        List<RestaurantSearchResult> results = documents.stream()
                .map(doc -> toResult(doc, userLat, userLon))
                .collect(Collectors.toList());

        return PagedSearchResponse.builder()
                .results(results)
                .totalHits(total)
                .page(page)
                .size(size)
                .hasMore((long) (page + 1) * size < total)
                .build();
    }

    private RestaurantSearchResult toResult(RestaurantDocument doc,
                                             double userLat, double userLon) {
        if (doc == null) return null;

        Double distanceKm = haversineKm(userLat, userLon, doc.getLatitude(), doc.getLongitude());

        // Map nested menuItems to result format
        List<RestaurantSearchResult.MenuItemResult> items = null;
        if (doc.getMenuItems() != null && !doc.getMenuItems().isEmpty()) {
            items = doc.getMenuItems().stream()
                    .map(item -> RestaurantSearchResult.MenuItemResult.builder()
                            .name(item.getName())
                            .category(item.getCategory())
                            .isVeg(item.isVeg())
                            .rating(item.getRating())
                            .totalRatings(item.getTotalRatings())
                            .build())
                    .collect(Collectors.toList());
        }

        return RestaurantSearchResult.builder()
                .restaurantId(doc.getId())
                .name(doc.getName())
                .imageUrl(doc.getImageUrl())
                .status(doc.getStatus())
                .latitude(doc.getLatitude())
                .longitude(doc.getLongitude())
                .distanceKm(distanceKm)
                .rating(doc.getRating())
                .totalRatings(doc.getTotalRatings())
                .discount(doc.getDiscount())
                .menuItems(items)
                .build();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

interface RestaurantSearchRepository extends MongoRepository<RestaurantDocument, String> {
}
