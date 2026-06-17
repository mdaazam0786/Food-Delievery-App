package com.foodzie.restaurant_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class BulkMenuImportResponse {
    private String restaurantId;
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<MenuImportResult> results;

    @Data
    @Builder
    @AllArgsConstructor
    public static class MenuImportResult {
        private int rowNumber;
        private String itemName;
        private String status; // SUCCESS or FAILED
        private String message;
        private String itemId; // Set if successful
    }
}
