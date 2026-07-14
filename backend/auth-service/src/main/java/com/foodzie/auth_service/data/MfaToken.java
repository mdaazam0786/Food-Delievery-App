package com.foodzie.auth_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "mfa_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaToken {

    @Id
    private String id;

    private String userId;

    private String token;

    private MfaTokenType type;

    @Builder.Default
    private boolean used = false;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
