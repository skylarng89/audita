package io.audita.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates JWT access tokens.
 * Tokens carry: sub (userId), email, role, tenantSlug (null for Super Admin).
 */
@Service
public class JwtService {

    private static final String STREAM_TOKEN_TYPE = "NOTIFICATION_STREAM";

    private final SecretKey signingKey;
    private final long expirySeconds;
    private final long streamExpirySeconds;

    public JwtService(
            @Value("${audita.jwt.secret}") String secret,
            @Value("${audita.jwt.expiry-seconds:900}") long expirySeconds,
            @Value("${audita.jwt.stream-expiry-seconds:900}") long streamExpirySeconds) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret is required.");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirySeconds = expirySeconds;
        this.streamExpirySeconds = streamExpirySeconds;
    }

    public String issue(UUID userId, String email, String role, String tenantSlug) {
        return issue(userId, email, role, List.of(role), List.of(), tenantSlug);
    }

    public String issue(UUID userId,
            String email,
            String role,
            List<String> roles,
            List<String> permissions,
            String tenantSlug) {
        Instant now = Instant.now();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("email", email);
        claims.put("role", role);
        claims.put("roles", roles == null ? List.of(role) : new ArrayList<>(roles));
        claims.put("permissions", permissions == null ? List.of() : new ArrayList<>(permissions));
        claims.put("tenantSlug", tenantSlug != null ? tenantSlug : "");

        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    public String issueStreamToken(UUID userId, String tenantSlug) {
        Instant now = Instant.now();
        Map<String, Object> claims = Map.of(
                "tokenType", STREAM_TOKEN_TYPE,
                "tenantSlug", tenantSlug != null ? tenantSlug : "");

        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(streamExpirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    public boolean isValidStreamToken(String token) {
        try {
            Claims claims = parse(token);
            return STREAM_TOKEN_TYPE.equals(claims.get("tokenType", String.class));
        } catch (Exception _) {
            return false;
        }
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
        } catch (Exception _) {
            return false;
        }
    }
}
