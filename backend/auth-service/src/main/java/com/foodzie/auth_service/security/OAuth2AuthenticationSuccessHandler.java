package com.foodzie.auth_service.security;

import com.foodzie.auth_service.data.User;
import com.foodzie.auth_service.repository.UserRepository;
import com.foodzie.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;
    private final UserRepository userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider tokenProvider,
                                              @Lazy AuthService authService,
                                              UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            log.info("OAuth2 authentication success, building redirect URL...");
            UserPrincipal principal = resolvePrincipal(authentication.getPrincipal());
            log.info("Principal resolved: {}", principal.getUsername());
            
            String accessToken = tokenProvider.generateAccessToken(principal);
            String refreshToken = authService.createRefreshToken(principal, request);
            log.info("Tokens generated. Access token length: {}, Refresh token length: {}", 
                accessToken.length(), refreshToken.length());

            String targetUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl + "/auth/callback")
                    .queryParam("token", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();

            log.info("Redirecting to: {}", targetUrl);

            if (response.isCommitted()) {
                log.debug("Response already committed, cannot redirect to {}", targetUrl);
                return;
            }
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception ex) {
            log.error("OAuth2 success handler failed: {}", ex.getMessage(), ex);
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl + "/login")
                    .queryParam("error", "Sign-in failed. Please try again.")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    /**
     * Google (OIDC) yields {@link OidcUser}; GitHub yields {@link UserPrincipal}.
     * Both paths provision the local user before this handler runs.
     */
    private UserPrincipal resolvePrincipal(Object authPrincipal) {
        if (authPrincipal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        String email = extractEmail(authPrincipal);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "OAuth user not found after provisioning: " + email));

        return new UserPrincipal(user);
    }

    private String extractEmail(Object authPrincipal) {
        if (authPrincipal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        if (authPrincipal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttribute("email");
            if (email instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        throw new IllegalStateException(
                "Cannot extract email from OAuth principal: " + authPrincipal.getClass().getName());
    }
}
