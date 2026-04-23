package io.audita.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates JWT access tokens.
 * Tokens carry: sub (userId), email, role, tenantSlug (null for Super Admin).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirySeconds;

    public JwtService(
            @Value("${audita.jwt.secret}") String secret,
            @Value("${audita.jwt.expiry-seconds:900}") long expirySeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirySeconds = expirySeconds;
    }

    public String issue(UUID userId, String email, String role, String tenantSlug) {
        Instant now = Instant.now();
        Map<String, Object> claims = Map.of(
                "email", email,
                "role", role,
                "tenantSlug", tenantSlug != null ? tenantSlug : ""
        );

        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
