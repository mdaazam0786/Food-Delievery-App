package com.foodzie.auth_service.service;

public interface MfaService {

    /** Enable TOTP-based MFA for a user; returns the TOTP secret URI for QR code generation */
    String enableTotp(String userId);

    /** Verify a TOTP code and activate MFA if valid */
    void confirmTotp(String userId, String totpCode);

    void disableMfa(String userId);

    /** Send an email OTP for MFA challenge */
    void sendEmailOtp(String userId);

    /** Verify an email OTP */
    boolean verifyEmailOtp(String userId, String otp);
}
