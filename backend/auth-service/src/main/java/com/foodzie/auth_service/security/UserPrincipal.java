package com.foodzie.auth_service.security;

import com.foodzie.auth_service.data.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class UserPrincipal implements UserDetails, OAuth2User {

    private final Long id;
    private final String email;
    private final String username;
    private final String password;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public UserPrincipal(User user) {
        this.id         = user.getId();
        this.email      = user.getEmail();
        this.username   = user.getUsername();
        this.password   = user.getPasswordHash();
        this.active     = user.isActive();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    // ── UserDetails ──────────────────────────────────────────────────────────

    @Override public String getPassword()              { return password; }
    @Override public String getUsername()              { return username; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return active; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }

    // ── OAuth2User ───────────────────────────────────────────────────────────

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName()                    { return email; }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
