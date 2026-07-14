package com.foodzie.auth_service.service;

import com.foodzie.auth_service.dto.*;
import com.foodzie.auth_service.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest);

    void logout(String refreshToken, String userId);

    void logoutAll(String userId);

    void initiatePasswordReset(PasswordResetRequest request);

    void confirmPasswordReset(PasswordResetConfirmRequest request);

    AuthResponse verifyMfa(MfaVerifyRequest request, HttpServletRequest httpRequest);

    /** Called by OAuth2 success handler to persist a refresh token */
    String createRefreshToken(UserPrincipal principal, HttpServletRequest request);

    /** Bulk register multiple users (admin only) */
    BulkUserImportResponse bulkRegister(BulkUserImportRequest request);
}
