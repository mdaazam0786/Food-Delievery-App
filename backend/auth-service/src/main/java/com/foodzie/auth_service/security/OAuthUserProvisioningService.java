package com.foodzie.auth_service.security;

import com.foodzie.auth_service.data.OAuthConnection;
import com.foodzie.auth_service.data.User;
import com.foodzie.auth_service.data.UserRole;
import com.foodzie.auth_service.data.UserStatus;
import com.foodzie.auth_service.repository.OAuthConnectionRepository;
import com.foodzie.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthUserProvisioningService {

    private final UserRepository userRepository;
    private final OAuthConnectionRepository oAuthConnectionRepository;

    @Transactional
    public User processOAuthUser(String provider, OAuth2UserInfo userInfo) {
        Optional<OAuthConnection> existingConnection =
                oAuthConnectionRepository.findByProviderAndProviderUserId(provider, userInfo.getId());

        if (existingConnection.isPresent()) {
            OAuthConnection conn = existingConnection.get();
            return userRepository.findById(conn.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }

        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
        User user = existingUser.orElseGet(() -> provisionNewOAuthUser(userInfo));

        OAuthConnection connection = OAuthConnection.builder()
                .userId(user.getId())
                .provider(provider)
                .providerUserId(userInfo.getId())
                .build();
        oAuthConnectionRepository.save(connection);

        return user;
    }

    private User provisionNewOAuthUser(OAuth2UserInfo userInfo) {
        String username = generateUsername(userInfo.getEmail());

        User user = User.builder()
                .email(userInfo.getEmail())
                .username(username)
                .fullName(userInfo.getFullName())
                .profilePictureUrl(userInfo.getImageUrl())
                .role(UserRole.ROLE_USER)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
