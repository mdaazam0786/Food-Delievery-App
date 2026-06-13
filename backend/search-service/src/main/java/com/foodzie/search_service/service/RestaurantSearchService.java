package com.foodzie.search_service.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ScriptSortType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.foodzie.search_service.document.RestaurantDocument;
import com.foodzie.search_service.dto.PagedSearchResponse;
import com.foodzie.search_service.dto.RestaurantSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Executes the two Elasticsearch queries powering the Foodzie discovery experience.
 *
 * The "restaurants" index is written by restaurant-service using flat latitude/longitude
 * double fields (not a geo_point). Geo queries are expressed via Painless scripts.
 *
 * ── Query 1: Nearby Feed ─────────────────────────────────────────────────────
 * GET /api/search/nearby
 * Script filter: Haversine distance ≤ radiusKm, sorted by computed distance asc.
 *
 * ── Query 2: Universal Search with Geo-Decay ─────────────────────────────────
 * GET /api/search?q=...
 * Fuzzy text match on name + menuItemNames, re-ranked via function_score
 * with a Gaussian decay script so nearby results score higher.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantSearchService {

    private static final String INDEX = "restaurants";

    // Haversine distance in km between (params.lat, params.lon) and stored doc lat/lon.
    private static final String HAVERSINE_SCRIPT =
            "double lat1 = Math.toRadians(params.lat);" +
            "double lon1 = Math.toRadians(params.lon);" +
            "double lat2 = Math.toRadians(doc['latitude'].value);" +
            "double lon2 = Math.toRadians(doc['longitude'].value);" +
            "double dlat = lat2 - lat1; double dlon = lon2 - lon1;" +
            "double a = Math.sin(dlat/2)*Math.sin(dlat/2) +" +
            " Math.cos(lat1)*Math.cos(lat2)*Math.sin(dlon/2)*Math.sin(dlon/2);" +
            "return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));";

    private final ElasticsearchClient esClient;

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
                                      int page, int size) throws IOException {
        int resolvedSize = size > 0 ? size : defaultPageSize;
        int from = page * resolvedSize;

        // Filter script: distance <= radiusKm
        String filterSrc = HAVERSINE_SCRIPT.replace(
                "return 6371.0",
                "double dist = 6371.0") +
                " return dist <= params.radius;";
        // Fix: HAVERSINE_SCRIPT already ends with "return ...", rebuild properly
        String filterScript =
                "double lat1 = Math.toRadians(params.lat);" +
                "double lon1 = Math.toRadians(params.lon);" +
                "double lat2 = Math.toRadians(doc['latitude'].value);" +
                "double lon2 = Math.toRadians(doc['longitude'].value);" +
                "double dlat = lat2 - lat1; double dlon = lon2 - lon1;" +
                "double a = Math.sin(dlat/2)*Math.sin(dlat/2) +" +
                " Math.cos(lat1)*Math.cos(lat2)*Math.sin(dlon/2)*Math.sin(dlon/2);" +
                "double dist = 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));" +
                "return dist <= params.radius;";

        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX)
                .from(from)
                .size(resolvedSize)
                .query(q -> q
                        .bool(b -> b
                                .filter(f -> f.term(t -> t.field("status").value("OPEN")))
                                .filter(f -> f.script(sc -> sc
                                        .script(scr -> scr
                                                .source(filterScript)
                                                .params("lat",    JsonData.of(lat))
                                                .params("lon",    JsonData.of(lon))
                                                .params("radius", JsonData.of(radiusKm))
                                        )
                                ))
                        )
                )
                .sort(so -> so
                        .script(ss -> ss
                                .type(ScriptSortType.Number)
                                .script(scr -> scr
                                        .source(HAVERSINE_SCRIPT)
                                        .params("lat", JsonData.of(lat))
                                        .params("lon", JsonData.of(lon))
                                )
                                .order(SortOrder.Asc)
                        )
                )
        );

        try {
            SearchResponse<RestaurantDocument> response =
                    esClient.search(request, RestaurantDocument.class);
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            log.debug("Nearby query lat={} lon={} radius={}km page={} size={} hits={}",
                    lat, lon, radiusKm, page, resolvedSize, total);

            return toPagedResponse(response, page, resolvedSize, total, lat, lon);
        } catch (Exception e) {
            log.error("Full error: ", e);  // this prints the entire stack trace
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                log.error("Caused by: ", cause);
            }
            throw e;
        }
    }

    // ── Query 2: Universal Search with Geo-Decay ─────────────────────────────

    public PagedSearchResponse search(String query, double lat, double lon,
                                      double radiusKm, int page, int size) throws IOException {
        int resolvedSize = size > 0 ? size : defaultPageSize;
        int from = page * resolvedSize;

        // Radius filter script
        String filterScript =
                "double lat1 = Math.toRadians(params.lat);" +
                "double lon1 = Math.toRadians(params.lon);" +
                "double lat2 = Math.toRadians(doc['latitude'].value);" +
                "double lon2 = Math.toRadians(doc['longitude'].value);" +
                "double dlat = lat2 - lat1; double dlon = lon2 - lon1;" +
                "double a = Math.sin(dlat/2)*Math.sin(dlat/2) +" +
                " Math.cos(lat1)*Math.cos(lat2)*Math.sin(dlon/2)*Math.sin(dlon/2);" +
                "double dist = 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));" +
                "return dist <= params.radius;";

        // Gaussian decay script: multiplier based on distance from user
        String decayScript =
                "double lat1 = Math.toRadians(params.lat);" +
                "double lon1 = Math.toRadians(params.lon);" +
                "double lat2 = Math.toRadians(doc['latitude'].value);" +
                "double lon2 = Math.toRadians(doc['longitude'].value);" +
                "double dlat = lat2 - lat1; double dlon = lon2 - lon1;" +
                "double a = Math.sin(dlat/2)*Math.sin(dlat/2) +" +
                " Math.cos(lat1)*Math.cos(lat2)*Math.sin(dlon/2)*Math.sin(dlon/2);" +
                "double dist = 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));" +
                "double offset = params.offset; double scale = params.scale; double decay = params.decay;" +
                "double sigma2 = -(scale * scale) / (2.0 * Math.log(decay));" +
                "double d = Math.max(0.0, dist - offset);" +
                "return Math.exp(-(d * d) / (2.0 * sigma2));";

        BoolQuery textQuery = BoolQuery.of(b -> b
                .should(s -> s.match(m -> m
                        .field("name").query(query).fuzziness("AUTO").boost(2.0f)))
                .should(s -> s.nested(n -> n
                        .path("menuItems")
                        .query(q -> q.match(m -> m
                                .field("menuItems.name").query(query).fuzziness("AUTO")))))
                .should(s -> s.nested(n -> n
                        .path("menuItems")
                        .query(q -> q.match(m -> m
                                .field("menuItems.description").query(query).fuzziness("AUTO")))))
        );

        FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.of(fs -> fs
                .query(q -> q.bool(textQuery))
                .functions(fn -> fn
                        .filter(f -> f.matchAll(ma -> ma))
                        .scriptScore(ss -> ss
                                .script(scr -> scr
                                        .source(decayScript)
                                        .params("lat",    JsonData.of(lat))
                                        .params("lon",    JsonData.of(lon))
                                        .params("scale",  JsonData.of(decayScaleKm))
                                        .params("offset", JsonData.of(decayOffsetKm))
                                        .params("decay",  JsonData.of(decayValue))
                                )
                        )
                )
                .scoreMode(FunctionScoreMode.Multiply)
                .boostMode(FunctionBoostMode.Multiply)
        );

        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX)
                .from(from)
                .size(resolvedSize)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t.field("status").value("OPEN")))
                                .must(m -> m.functionScore(functionScoreQuery))
                                .filter(f -> f.script(sc -> sc
                                        .script(scr -> scr
                                                .source(filterScript)
                                                .params("lat",    JsonData.of(lat))
                                                .params("lon",    JsonData.of(lon))
                                                .params("radius", JsonData.of(radiusKm))
                                        )
                                ))
                        )
                )
        );

        SearchResponse<RestaurantDocument> response =
                esClient.search(request, RestaurantDocument.class);

        long total = response.hits().total() != null ? response.hits().total().value() : 0;
        log.debug("Search query='{}' lat={} lon={} radius={}km page={} size={} hits={}",
                query, lat, lon, radiusKm, page, resolvedSize, total);

        return toPagedResponse(response, page, resolvedSize, total, lat, lon);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private PagedSearchResponse toPagedResponse(SearchResponse<RestaurantDocument> response,
                                                 int page, int size, long total,
                                                 double userLat, double userLon) {
        List<RestaurantSearchResult> results = response.hits().hits().stream()
                .map(hit -> toResult(hit, userLat, userLon))
                .collect(Collectors.toList());

        return PagedSearchResponse.builder()
                .results(results)
                .totalHits(total)
                .page(page)
                .size(size)
                .hasMore((long) (page + 1) * size < total)
                .build();
    }

    private RestaurantSearchResult toResult(Hit<RestaurantDocument> hit,
                                             double userLat, double userLon) {
        RestaurantDocument doc = hit.source();
        if (doc == null) return null;

        // Use sort value (distance from nearby sort script) when present, else compute it
        Double distanceKm = null;
        if (hit.sort() != null && !hit.sort().isEmpty()) {
            try {
                distanceKm = Double.parseDouble(hit.sort().get(0).toString());
            } catch (NumberFormatException ignored) { }
        }
        if (distanceKm == null) {
            distanceKm = haversineKm(userLat, userLon, doc.getLatitude(), doc.getLongitude());
        }

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
                .restaurantId(hit.id())
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
