package com.foodzie.auth_service.service;

public interface MfaService {

    /** Enable TOTP-based MFA for a user; returns the TOTP secret URI for QR code generation */
    String enableTotp(Long userId);

    /** Verify a TOTP code and activate MFA if valid */
    void confirmTotp(Long userId, String totpCode);

    void disableMfa(Long userId);

    /** Send an email OTP for MFA challenge */
    void sendEmailOtp(Long userId);

    /** Verify an email OTP */
    boolean verifyEmailOtp(Long userId, String otp);
}
