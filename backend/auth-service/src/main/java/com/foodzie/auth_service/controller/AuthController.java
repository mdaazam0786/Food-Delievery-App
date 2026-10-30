package com.foodzie.auth_service.controller;

import com.foodzie.auth_service.dto.*;
import com.foodzie.auth_service.service.AuthService;
import com.foodzie.auth_service.utils.SecurityUtils;
import com.foodzie.common.dto.ApiResponse;
import com.foodzie.common.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Register a new user (public) */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.debug("Received user registration request for email: {}", request.getEmail());
            
            // Validate input
            if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
                log.warn("Registration request validation failed: email is required");
                throw BadRequestException.of("email", "Email is required");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                log.warn("Registration request validation failed: password is required");
                throw BadRequestException.of("password", "Password is required");
            }
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                log.warn("Registration request validation failed: username is required");
                throw BadRequestException.of("username", "Username is required");
            }
            
            AuthResponse response = authService.register(request);
            log.info("User registered successfully with email: {}", request.getEmail());
            return ResponseEntity.status(201).body(ApiResponse.ok("User registered successfully", response));
            
        } catch (BadRequestException e) {
            log.warn("Bad request during registration: {}", e.getMessage());
            throw e;
        } catch (ConflictException e) {
            log.warn("Conflict error during registration: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during user registration", e);
            throw new InternalServerException("User registration failed", e);
        }
    }

    /** Login with email/username + password (public) */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        try {
            log.info("Login attempt for user: {}", request.getEmailOrUsername());
            
            // Validate input
            if (request == null) {
                log.warn("Login request validation failed: request is null");
                throw new BadRequestException("Login request cannot be empty");
            }
            if (request.getEmailOrUsername() == null || request.getEmailOrUsername().isBlank()) {
                log.warn("Login request validation failed: email or username is required");
                throw BadRequestException.of("emailOrUsername", "Email or username is required");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                log.warn("Login request validation failed: password is required");
                throw BadRequestException.of("password", "Password is required");
            }
            
            AuthResponse response = authService.login(request, httpRequest);
            log.info("User login successful");
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (BadRequestException e) {
            log.warn("Bad request during login: {}", e.getMessage());
            throw e;
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized login attempt: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            throw new InternalServerException("Login failed", e);
        }
    }

    /** Complete MFA challenge (public – uses challenge token) */
    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfa(@Valid @RequestBody MfaVerifyRequest request,
                                                                HttpServletRequest httpRequest) {
        try {
            log.info("MFA verification attempt");
            
            // Validate input
            if (request == null) {
                log.warn("MFA verification request validation failed: request is null");
                throw new BadRequestException("MFA verification request cannot be empty");
            }
            if (request.getChallengeToken() == null || request.getChallengeToken().isBlank()) {
                log.warn("MFA verification request validation failed: challenge token is required");
                throw BadRequestException.of("challengeToken", "Challenge token is required");
            }
            if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
                log.warn("MFA verification request validation failed: OTP code is required");
                throw BadRequestException.of("otpCode", "OTP code is required");
            }
            
            AuthResponse response = authService.verifyMfa(request, httpRequest);
            log.info("MFA verification successful");
            return ResponseEntity.ok(ApiResponse.ok("MFA verified", response));
            
        } catch (BadRequestException e) {
            log.warn("Bad request during MFA verification: {}", e.getMessage());
            throw e;
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized MFA verification attempt: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during MFA verification", e);
            throw new InternalServerException("MFA verification failed", e);
        }
    }

    /** Refresh access token (public – uses refresh token) */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                              HttpServletRequest httpRequest) {
        try {
            log.debug("Token refresh attempt");
            
            // Validate input
            if (request == null) {
                log.warn("Refresh token request validation failed: request is null");
                throw new BadRequestException("Refresh token request cannot be empty");
            }
            if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
                log.warn("Refresh token request validation failed: refresh token is required");
                throw BadRequestException.of("refreshToken", "Refresh token is required");
            }
            
            AuthResponse response = authService.refreshToken(request, httpRequest);
            log.debug("Token refresh successful");
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (BadRequestException e) {
            log.warn("Bad request during token refresh: {}", e.getMessage());
            throw e;
        } catch (UnauthorizedException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            throw new InternalServerException("Token refresh failed", e);
        }
    }

    /** Logout current session (authenticated) */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.info("Logout attempt");
            
            // Validate input
            if (request == null) {
                log.warn("Logout request validation failed: request is null");
                throw new BadRequestException("Logout request cannot be empty");
            }
            if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
                log.warn("Logout request validation failed: refresh token is required");
                throw BadRequestException.of("refreshToken", "Refresh token is required");
            }
            
            String userId = SecurityUtils.getCurrentUserId();
            authService.logout(request.getRefreshToken(), userId);
            log.info("User logout successful");
            return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
            
        } catch (BadRequestException e) {
            log.warn("Bad request during logout: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during logout", e);
            throw new InternalServerException("Logout failed", e);
        }
    }

    /** Logout all sessions (authenticated) */
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        try {
            log.info("Logout all sessions attempt");
            
            String userId = SecurityUtils.getCurrentUserId();
            authService.logoutAll(userId);
            log.info("All user sessions terminated successfully");
            return ResponseEntity.ok(ApiResponse.ok("All sessions terminated", null));
            
        } catch (Exception e) {
            log.error("Unexpected error during logout all", e);
            throw new InternalServerException("Logout all failed", e);
        }
    }

    /** Initiate password reset (public) */
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> initiatePasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        try {
            log.info("Password reset initiation attempt");
            
            // Validate input
            if (request == null) {
                log.warn("Password reset request validation failed: request is null");
                throw new BadRequestException("Password reset request cannot be empty");
            }
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                log.warn("Password reset request validation failed: email is required");
                throw BadRequestException.of("email", "Email is required");
            }
            
            authService.initiatePasswordReset(request);
            log.info("Password reset link sent");
            return ResponseEntity.ok(ApiResponse.ok("If that email exists, a reset link has been sent", null));
            
        } catch (BadRequestException e) {
            log.warn("Bad request during password reset initiation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during password reset initiation", e);
            throw new InternalServerException("Password reset initiation failed", e);
        }
    }

    /** Confirm password reset with token (public) */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            log.info("Password reset confirmation attempt");
            
            // Validate input
            if (request == null) {
                log.warn("Password reset confirmation request validation failed: request is null");
                throw new BadRequestException("Password reset confirmation request cannot be empty");
            }
            if (request.getToken() == null || request.getToken().isBlank()) {
                log.warn("Password reset confirmation request validation failed: token is required");
                throw BadRequestException.of("token", "Reset token is required");
            }
            if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
                log.warn("Password reset confirmation request validation failed: new password is required");
                throw BadRequestException.of("newPassword", "New password is required");
            }
            
            authService.confirmPasswordReset(request);
            log.info("Password reset confirmed successfully");
            return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
            
        } catch (BadRequestException e) {
            log.warn("Bad request during password reset confirmation: {}", e.getMessage());
            throw e;
        } catch (UnauthorizedException e) {
            log.warn("Invalid reset token: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during password reset confirmation", e);
            throw new InternalServerException("Password reset confirmation failed", e);
        }
    }
}
