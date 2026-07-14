package com.foodzie.auth_service.controller;

import com.foodzie.auth_service.dto.*;
import com.foodzie.auth_service.service.AuthService;
import com.foodzie.auth_service.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Register a new user (public) */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(201).body(ApiResponse.ok("User registered successfully", response));
    }

    /** Login with email/username + password (public) */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** Complete MFA challenge (public – uses challenge token) */
    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfa(@Valid @RequestBody MfaVerifyRequest request,
                                                                HttpServletRequest httpRequest) {
        AuthResponse response = authService.verifyMfa(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("MFA verified", response));
    }

    /** Refresh access token (public – uses refresh token) */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                              HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** Logout current session (authenticated) */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        authService.logout(request.getRefreshToken(), userId);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    /** Logout all sessions (authenticated) */
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        String userId = SecurityUtils.getCurrentUserId();
        authService.logoutAll(userId);
        return ResponseEntity.ok(ApiResponse.ok("All sessions terminated", null));
    }

    /** Initiate password reset (public) */
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> initiatePasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.initiatePasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok("If that email exists, a reset link has been sent", null));
    }

    /** Confirm password reset with token (public) */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }
}
