package com.foodzie.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUserImportResponse {

    private int totalUsers;
    private int successCount;
    private int failureCount;
    private List<UserImportResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserImportResult {
        private int rowNumber;
        private String email;
        private String username;
        private String status; // SUCCESS, FAILED, SKIPPED
        private String message;
        private String userId; // only present on success
    }
}
