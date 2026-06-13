package com.foodzie.auth_service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Google registers {@code openid} scope, so Spring uses {@link OidcUserService}
 * instead of {@link OAuth2UserServiceImpl}. Provisions the local user here and
 * returns the standard {@link OidcUser} — the success handler resolves
 * {@link UserPrincipal} from the authenticated email.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OidcUserServiceImpl extends OidcUserService {

    private final OAuthUserProvisioningService provisioningService;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oidcUser.getAttributes();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, attributes);
        provisioningService.processOAuthUser(provider, userInfo);

        log.debug("OIDC user provisioned for provider={} email={}", provider, userInfo.getEmail());
        return oidcUser;
    }
}
