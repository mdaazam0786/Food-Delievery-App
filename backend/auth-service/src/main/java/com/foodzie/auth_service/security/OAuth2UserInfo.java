package com.foodzie.auth_service.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class OAuth2UserInfo {
    protected java.util.Map<String, Object> attributes;

    public abstract String getId();
    public abstract String getEmail();
    public abstract String getFullName();
    public abstract String getImageUrl();
}
