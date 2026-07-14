package com.foodzie.auth_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true)
    private String username;

    private String passwordHash;

    private String fullName;

    private String phoneNumber;

    private String profilePictureUrl;

    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private boolean mfaEnabled = false;

    /** TOTP secret – should be encrypted at rest in production */
    private String mfaSecret;

    private String passwordResetToken;

    private LocalDateTime passwordResetExpiry;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }
}
