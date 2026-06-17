package com.foodzie.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class BulkRestaurantImportResponse {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<RestaurantImportResult> results;

    @Data
    @Builder
    @AllArgsConstructor
    public static class RestaurantImportResult {
        private int rowNumber;
        private String name;
        private String status; // SUCCESS or FAILED
        private String message;
        private String restaurantId; // Set if successful
    }
}
