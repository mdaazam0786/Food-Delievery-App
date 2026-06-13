package com.foodzie.auth_service.service.implementation;

import com.foodzie.auth_service.data.*;
import com.foodzie.auth_service.dto.*;
import com.foodzie.auth_service.event.UserEventPublisher;
import com.foodzie.auth_service.event.UserRegisteredEvent;
import com.foodzie.auth_service.repository.RefreshTokenRepository;
import com.foodzie.auth_service.repository.UserRepository;
import com.foodzie.auth_service.security.JwtTokenProvider;
import com.foodzie.auth_service.security.UserPrincipal;
import com.foodzie.auth_service.service.AuditService;
import com.foodzie.auth_service.service.AuthService;
import com.foodzie.auth_service.service.MfaService;
import com.foodzie.auth_service.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final MfaService mfaService;
    private final AuditService auditService;
    private final UserEventPublisher userEventPublisher;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        String roleName = (request.getRole() != null) ? request.getRole() : "ROLE_USER";
        UserRole role;
        try {
            role = UserRole.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName + ". Must be ROLE_USER, ROLE_RESTAURANT, or ROLE_DRIVER");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        userRepository.save(user);

        auditService.log(user.getId(), user.getEmail(), "USER_REGISTER", "User",
                String.valueOf(user.getId()), null, null,
                AuditLog.AuditStatus.SUCCESS, Map.of("username", user.getUsername()));

        // Publish Kafka event so user-service can eagerly create the profile
        // and restaurant-service can auto-provision a stub for ROLE_RESTAURANT users
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
        userEventPublisher.publishUserRegistered(event);

        UserPrincipal principal = new UserPrincipal(user);
        return buildAuthResponse(principal, null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmailOrUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        auditService.log(user.getId(), user.getEmail(), "USER_LOGIN", "User",
                String.valueOf(user.getId()), SecurityUtils.getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"), AuditLog.AuditStatus.SUCCESS, null);

        // If MFA is enabled, return a challenge token instead of a full access token
        if (user.isMfaEnabled()) {
            mfaService.sendEmailOtp(user.getId());
            String challengeToken = tokenProvider.generateMfaChallengeToken(user.getId(), user.getEmail());
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .mfaChallengeToken(challengeToken)
                    .tokenType("Bearer")
                    .build();
        }

        String refreshToken = createRefreshToken(principal, httpRequest);
        return buildAuthResponse(principal, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse verifyMfa(MfaVerifyRequest request, HttpServletRequest httpRequest) {
        if (!tokenProvider.validateToken(request.getChallengeToken())) {
            throw new BadCredentialsException("Invalid or expired challenge token");
        }
        String tokenType = tokenProvider.getTokenType(request.getChallengeToken());
        if (!"MFA_CHALLENGE".equals(tokenType)) {
            throw new BadCredentialsException("Invalid challenge token type");
        }

        Long userId = tokenProvider.getUserIdFromToken(request.getChallengeToken());
        mfaService.verifyEmailOtp(userId, request.getOtpCode());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        UserPrincipal principal = new UserPrincipal(user);
        String refreshToken = createRefreshToken(principal, httpRequest);

        auditService.log(userId, user.getEmail(), "MFA_VERIFY", "User",
                String.valueOf(userId), SecurityUtils.getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"), AuditLog.AuditStatus.SUCCESS, null);

        return buildAuthResponse(principal, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        User user = stored.getUser();
        UserPrincipal principal = new UserPrincipal(user);
        String newAccessToken = tokenProvider.generateAccessToken(principal);

        // Rotate refresh token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        String newRefreshToken = createRefreshToken(principal, httpRequest);

        return buildAuthResponse(principal, newRefreshToken, newAccessToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, Long userId) {
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        auditService.log(userId, null, "USER_LOGOUT", "User",
                String.valueOf(userId), null, null, AuditLog.AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        auditService.log(userId, null, "USER_LOGOUT_ALL", "User",
                String.valueOf(userId), null, null, AuditLog.AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional
    public void initiatePasswordReset(PasswordResetRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            // TODO: send reset email with token
            log.info("Password reset token generated for {} (integrate email sender)", user.getEmail());
        });
        // Always return success to prevent email enumeration
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired reset token"));

        if (user.getPasswordResetExpiry() == null ||
                LocalDateTime.now().isAfter(user.getPasswordResetExpiry())) {
            throw new BadCredentialsException("Password reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        // Revoke all refresh tokens on password change
        refreshTokenRepository.revokeAllByUserId(user.getId());
        auditService.log(user.getId(), user.getEmail(), "PASSWORD_RESET", "User",
                String.valueOf(user.getId()), null, null, AuditLog.AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional
    public String createRefreshToken(UserPrincipal principal, HttpServletRequest request) {
        String rawToken = tokenProvider.generateRefreshToken(principal);
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(userRepository.getReferenceById(principal.getId()))
                .tokenHash(tokenHash)
                .deviceInfo(request != null ? request.getHeader("User-Agent") : null)
                .ipAddress(request != null ? SecurityUtils.getClientIp(request) : null)
                .expiresAt(LocalDateTime.now().plusSeconds(
                        tokenProvider.getRefreshTokenExpirationMs() / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(UserPrincipal principal, String refreshToken) {
        String accessToken = tokenProvider.generateAccessToken(principal);
        return buildAuthResponse(principal, refreshToken, accessToken);
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal, String refreshToken, String accessToken) {
        Set<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(principal.getId())
                .email(principal.getEmail())
                .username(principal.getUsername())
                .roles(roles)
                .mfaRequired(false)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
