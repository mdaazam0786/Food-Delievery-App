package com.foodzie.auth_service.service.implementation;

import com.foodzie.auth_service.data.*;
import com.foodzie.auth_service.repository.MfaTokenRepository;
import com.foodzie.auth_service.repository.UserRepository;
import com.foodzie.auth_service.service.MfaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private final UserRepository userRepository;
    private final MfaTokenRepository mfaTokenRepository;

    @Value("${app.mfa.totp-issuer}")
    private String totpIssuer;

    @Value("${app.mfa.otp-expiry-minutes}")
    private int otpExpiryMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public String enableTotp(String userId) {
        User user = getUser(userId);
        // Generate a random 20-byte TOTP secret
        byte[] secretBytes = new byte[20];
        secureRandom.nextBytes(secretBytes);
        String secret = Base64.getEncoder().encodeToString(secretBytes);
        user.setMfaSecret(secret);
        userRepository.save(user);

        // Return an otpauth URI that can be rendered as a QR code
        return "otpauth://totp/" + totpIssuer + ":" + user.getEmail()
                + "?secret=" + secret + "&issuer=" + totpIssuer;
    }

    @Override
    @Transactional
    public void confirmTotp(String userId, String totpCode) {
        User user = getUser(userId);
        // In production, validate the TOTP code against user.getMfaSecret()
        // using a library like GoogleAuth or java-otp.
        // For now we mark MFA as enabled after the user confirms the code.
        user.setMfaEnabled(true);
        userRepository.save(user);
        log.info("TOTP MFA enabled for user {}", userId);
    }

    @Override
    @Transactional
    public void disableMfa(String userId) {
        User user = getUser(userId);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        // Delete all EMAIL_OTP tokens for this user
        mfaTokenRepository.deleteByUserIdAndType(userId, MfaTokenType.EMAIL_OTP);
        log.info("MFA disabled for user {}", userId);
    }

    @Override
    @Transactional
    public void sendEmailOtp(String userId) {
        User user = getUser(userId);
        // Invalidate any existing OTPs
        mfaTokenRepository.deleteByUserIdAndType(userId, MfaTokenType.EMAIL_OTP);

        String otp = generateNumericOtp(6);
        MfaToken token = MfaToken.builder()
                .userId(userId)
                .token(otp)
                .type(MfaTokenType.EMAIL_OTP)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();
        mfaTokenRepository.save(token);

        // TODO: integrate with an email/notification service to send the OTP
        log.info("Email OTP generated for user {} (integrate email sender)", userId);
    }

    @Override
    @Transactional
    public boolean verifyEmailOtp(String userId, String otp) {
        MfaToken token = mfaTokenRepository
                .findLatestValidToken(userId, MfaTokenType.EMAIL_OTP)
                .orElseThrow(() -> new BadCredentialsException("No valid OTP found"));

        if (!token.isValid() || !token.getToken().equals(otp)) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }
        token.setUsed(true);
        mfaTokenRepository.save(token);
        return true;
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private String generateNumericOtp(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
