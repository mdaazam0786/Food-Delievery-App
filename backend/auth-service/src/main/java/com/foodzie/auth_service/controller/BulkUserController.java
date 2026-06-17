package com.foodzie.auth_service.controller;

import com.foodzie.auth_service.dto.ApiResponse;
import com.foodzie.auth_service.dto.BulkUserImportRequest;
import com.foodzie.auth_service.dto.BulkUserImportResponse;
import com.foodzie.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Bulk user import API for creating multiple users in a single request.
 * Requires ROLE_ADMIN authorization.
 * 
 * This endpoint allows admins to bulk create users and register them in the auth service.
 * Each user is validated, registered, and events are published for downstream services
 * (user-service, restaurant-service) to process.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/bulk")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class BulkUserController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/bulk/register
     * Bulk create multiple users and register them in the auth service.
     * 
     * Expected request body:
     * {
     *   "users": [
     *     {
     *       "email": "user1@example.com",
     *       "username": "user1",
     *       "password": "SecurePass123!",
     *       "fullName": "User One",
     *       "role": "ROLE_USER"  // Optional, defaults to ROLE_USER. Can be ROLE_USER, ROLE_RESTAURANT, ROLE_DRIVER, ROLE_ADMIN
     *     },
     *     ...
     *   ]
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "Users imported",
     *   "data": {
     *     "totalUsers": 3,
     *     "successCount": 2,
     *     "failureCount": 1,
     *     "results": [
     *       {
     *         "rowNumber": 1,
     *         "email": "user1@example.com",
     *         "username": "user1",
     *         "status": "SUCCESS",
     *         "message": "User created successfully",
     *         "userId": 101
     *       },
     *       ...
     *     ]
     *   }
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<BulkUserImportResponse>> bulkRegisterUsers(
            @Valid @RequestBody BulkUserImportRequest request) {
        
        try {
            log.info("Processing bulk user registration for {} users", 
                    request.getUsers() != null ? request.getUsers().size() : 0);
            
            BulkUserImportResponse response = authService.bulkRegister(request);
            
            return ResponseEntity.ok(ApiResponse.ok(
                    String.format("Users imported - Success: %d, Failure: %d", 
                            response.getSuccessCount(), response.getFailureCount()),
                    response
            ));
            
        } catch (Exception e) {
            log.error("Error processing bulk user registration: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<BulkUserImportResponse>builder()
                            .success(false)
                            .message("Error processing bulk user registration: " + e.getMessage())
                            .build());
        }
    }
}
