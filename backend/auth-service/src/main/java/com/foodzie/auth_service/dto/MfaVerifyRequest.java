package com.foodzie.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaVerifyRequest {

    /** The short-lived challenge token returned during login when MFA is required */
    @NotBlank(message = "Challenge token is required")
    private String challengeToken;

    /** 6-digit TOTP code or OTP */
    @NotBlank(message = "OTP code is required")
    private String otpCode;
}
