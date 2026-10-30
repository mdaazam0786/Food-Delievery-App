package com.foodzie.user_service.controller;

import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
import com.foodzie.user_service.dto.AvatarResponse;
import com.foodzie.user_service.dto.UpdateUserRequest;
import com.foodzie.user_service.dto.UserResponse;
import com.foodzie.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users
     * Returns the full profile of the currently authenticated user.
     * Creates a bare profile on first access if one doesn't exist yet.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal Jwt jwt) {
        try {
            log.debug("Retrieving user profile");
            
            // Validate input
            if (jwt == null) {
                log.warn("JWT token is null");
                throw new UnauthorizedException("Authentication required");
            }
            
            String userId = jwt.getClaim("userId");
            String email = jwt.getSubject();
            
            if (userId == null || userId.isBlank()) {
                log.warn("User ID is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: userId claim required");
            }
            if (email == null || email.isBlank()) {
                log.warn("Email is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: email claim required");
            }
            
            UserResponse response = userService.getMe(userId, email);
            log.debug("User profile retrieved successfully");
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized access to user profile: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving user profile", e);
            throw new InternalServerException("Failed to retrieve user profile", e);
        }
    }

    /**
     * PUT /api/v1/users
     * Updates editable profile fields: fullName, phoneNumber, bio.
     * Email is immutable and is ignored even if sent.
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            log.info("Updating user profile");
            
            // Validate input
            if (jwt == null) {
                log.warn("JWT token is null");
                throw new UnauthorizedException("Authentication required");
            }
            if (request == null) {
                log.warn("Update request is null");
                throw new BadRequestException("Update request cannot be empty");
            }
            
            String userId = jwt.getClaim("userId");
            
            if (userId == null || userId.isBlank()) {
                log.warn("User ID is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: userId claim required");
            }
            
            UserResponse response = userService.updateMe(userId, request);
            log.info("User profile updated successfully");
            return ResponseEntity.ok(ApiResponse.ok("Profile updated", response));
            
        } catch (BadRequestException | UnauthorizedException e) {
            log.warn("Bad request during profile update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating user profile", e);
            throw new InternalServerException("Failed to update user profile", e);
        }
    }

    /**
     * POST /api/v1/users/avatar
     * Uploads a new profile picture (multipart/form-data, field name: "file").
     * Accepted types: JPEG, PNG, WebP — max 5 MB.
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AvatarResponse>> uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Avatar upload initiated");
            
            // Validate input
            if (jwt == null) {
                log.warn("JWT token is null");
                throw new UnauthorizedException("Authentication required");
            }
            if (file == null || file.isEmpty()) {
                log.warn("Avatar file is null or empty");
                throw BadRequestException.of("file", "File is required");
            }
            
            String userId = jwt.getClaim("userId");
            
            if (userId == null || userId.isBlank()) {
                log.warn("User ID is missing from JWT token");
                throw new UnauthorizedException("Invalid JWT: userId claim required");
            }
            
            AvatarResponse response = userService.uploadAvatar(userId, file);
            log.info("Avatar uploaded successfully");
            return ResponseEntity.ok(ApiResponse.ok("Avatar uploaded", response));
            
        } catch (BadRequestException | UnauthorizedException e) {
            log.warn("Bad request during avatar upload: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error uploading avatar", e);
            throw new InternalServerException("Failed to upload avatar", e);
        }
    }
}
