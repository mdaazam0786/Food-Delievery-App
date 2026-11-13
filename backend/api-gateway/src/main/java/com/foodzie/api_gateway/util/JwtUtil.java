package com.foodzie.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    private Key cachedSigningKey;

    /**
     * Builds and caches the signing key from the Base64-encoded secret in application.properties.
     * Caching prevents repeated Base64 decoding on every token validation.
     */
    private Key getSigningKey() {
        if (cachedSigningKey == null) {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return cachedSigningKey;
    }

    /**
     * Validates the token signature and expiry.
     * Throws JwtException / IllegalArgumentException if invalid.
     */
    public void validateToken(String token) {
        Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
    }

    /**
     * Extracts all claims from a valid token.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the subject (usually userId or email) from the token.
     */
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }
}
