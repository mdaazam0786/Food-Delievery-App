package com.foodzie.auth_service.controller;

import com.foodzie.auth_service.dto.*;
import com.foodzie.auth_service.service.AuthService;
import com.foodzie.auth_service.util.ExcelParsingUtil;
import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.BadRequestException;
import com.foodzie.common.exceptions.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bulk user import API for creating multiple users from Excel files.
 * Requires ROLE_ADMIN authorization.
 * 
 * This endpoint allows admins to bulk create users and register them in the auth service
 * by uploading an Excel file. Each user is validated, registered, and events are published 
 * for downstream services (user-service, restaurant-service) to process.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/bulk")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class BulkUserController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/bulk/import-excel
     * Upload an Excel file to bulk create and register multiple users.
     * 
     * Expected columns in Excel:
     * - email (required)
     * - username (required)
     * - password (required, min 8 characters)
     * - fullName (optional)
     * - role (optional, defaults to ROLE_USER) - Can be ROLE_USER, ROLE_RESTAURANT, ROLE_DRIVER, ROLE_ADMIN
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "Users imported - Success: 3, Failure: 0",
     *   "data": {
     *     "totalUsers": 3,
     *     "successCount": 3,
     *     "failureCount": 0,
     *     "results": [
     *       {
     *         "rowNumber": 2,
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
    @PostMapping("/import-excel")
    public ResponseEntity<ApiResponse<BulkUserImportResponse>> importUsersFromExcel(
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("Received bulk user import request");
            
            // Validate file
            if (file == null) {
                log.warn("File upload validation failed: file is null");
                throw new BadRequestException("File is required");
            }
            
            if (file.isEmpty()) {
                log.warn("File upload validation failed: file is empty");
                throw new BadRequestException("File is empty");
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                log.warn("File upload validation failed: filename is invalid");
                throw new BadRequestException("Invalid filename");
            }
            
            if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
                log.warn("File upload validation failed: unsupported file type - {}", filename);
                throw BadRequestException.of("file", "Only .xlsx and .xls files are supported");
            }
            
            log.info("Processing bulk user import from Excel file: {}", filename);
            
            // Parse Excel file
            List<Map<String, String>> userDataList = ExcelParsingUtil.parseUserExcel(file);
            
            if (userDataList == null || userDataList.isEmpty()) {
                log.warn("Excel file parsing resulted in empty user list");
                throw new BadRequestException("No users found in Excel file");
            }
            
            // Convert to BulkUserImportRequest
            List<BulkUserImportRequest.UserImportData> users = new ArrayList<>();
            for (Map<String, String> data : userDataList) {
                users.add(BulkUserImportRequest.UserImportData.builder()
                        .email(data.get("email"))
                        .username(data.get("username"))
                        .password(data.get("password"))
                        .fullName(data.get("fullName"))
                        .role(data.get("role"))
                        .build());
            }
            
            BulkUserImportRequest request = BulkUserImportRequest.builder()
                    .users(users)
                    .build();
            
            log.debug("Processing {} users from Excel file", users.size());
            
            // Process bulk registration
            BulkUserImportResponse response = authService.bulkRegister(request);
            
            log.info("Bulk user import completed - Success: {}, Failure: {}", 
                    response.getSuccessCount(), response.getFailureCount());
            
            return ResponseEntity.ok(ApiResponse.ok(
                    String.format("Users imported - Success: %d, Failure: %d", 
                            response.getSuccessCount(), response.getFailureCount()),
                    response
            ));
            
        } catch (BadRequestException e) {
            log.warn("Bad request during bulk user import: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("IO error reading Excel file: ", e);
            throw new BadRequestException("Error reading Excel file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during bulk user import", e);
            throw new InternalServerException("Bulk user import failed", e);
        }
    }
}
