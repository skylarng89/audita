package io.audita.infrastructure.service;

import io.audita.application.port.SsoPort;
import io.audita.domain.model.OAuthProvider;

/**
 * Stores SSO authorization state and frontend exchange tokens.
 *
 * The default {@link InMemorySsoStateStore} implementation uses
 * {@link java.util.concurrent.ConcurrentHashMap} and is suitable only for
 * single-instance deployments. For multi-instance high-availability,
 * replace this with a database-backed or Redis-backed implementation.
 */
public interface SsoStateStore {

    void putState(String stateToken, String tenantSlug, OAuthProvider provider, long expiresAt);

    SsoService.SsoState validateAndRemoveState(String stateToken);

    void putExchange(String code, SsoPort.SsoResult result, long expiresAt);

    SsoService.FrontendExchangeState consumeExchange(String code);

    void cleanupExpired();
}
