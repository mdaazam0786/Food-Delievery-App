package com.foodzie.user_service.controller;

import com.foodzie.user_service.dto.ApiResponse;
import com.foodzie.user_service.dto.AvatarResponse;
import com.foodzie.user_service.dto.UpdateUserRequest;
import com.foodzie.user_service.dto.UserResponse;
import com.foodzie.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        Long userId = jwt.getClaim("userId");
        String email = jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(userId, email)));
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
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", userService.updateMe(userId, request)));
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
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(ApiResponse.ok("Avatar uploaded", userService.uploadAvatar(userId, file)));
    }
}
