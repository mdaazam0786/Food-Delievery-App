package com.foodzie.auth_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import lombok.*;

import java.time.LocalDateTime;

@Document(collection = "oauth_connections")
@CompoundIndexes({
    @CompoundIndex(name = "provider_user_idx", def = "{'provider': 1, 'providerUserId': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthConnection {

    @Id
    private String id;

    private String userId;

    private String provider;

    private String providerUserId;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime tokenExpiry;

    private LocalDateTime connectedAt;
}
