package com.foodzie.search_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated wrapper returned by both search endpoints.
 * The React Native app uses totalHits to decide whether to show a "Load more" button.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedSearchResponse {

    private List<RestaurantSearchResult> results;

    /** Total number of matching documents in the index (before pagination). */
    private long totalHits;

    /** Current page number (0-indexed). */
    private int page;

    /** Number of results per page. */
    private int size;

    /** True if there are more pages after this one. */
    private boolean hasMore;
}
