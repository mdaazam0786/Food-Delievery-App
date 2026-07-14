package com.foodzie.auth_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;          // seconds
    private String userId;
    private String email;
    private String username;
    private Set<String> roles;

    /** True when MFA is required before the access token is fully valid */
    private boolean mfaRequired;
    private String mfaChallengeToken; // short-lived token used to complete MFA step
}
