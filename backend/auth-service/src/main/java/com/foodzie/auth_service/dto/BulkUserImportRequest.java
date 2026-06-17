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
public class BulkUserImportRequest {

    private List<UserImportData> users;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserImportData {
        private String email;
        private String username;
        private String password;
        private String fullName;
        private String role; // ROLE_USER, ROLE_RESTAURANT, ROLE_DRIVER, ROLE_ADMIN
    }
}
