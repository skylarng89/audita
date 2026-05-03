package io.audita.application.port;

import io.audita.domain.model.OAuthProvider;

import java.util.UUID;

public interface SsoPort {

    String buildAuthorizationUrl(String tenantSlug, OAuthProvider provider);

    SsoResult handleCallback(OAuthProvider provider, String code, String state);

    String issueFrontendExchangeCode(SsoResult result);

    SsoResult consumeFrontendExchangeCode(String code);

    record SsoResult(
            String accessToken,
            String rawRefreshToken,
            UUID userId,
            String email,
            String fullName,
            String role,
            String tenantSlug,
            long expiresIn
    ) {}
}