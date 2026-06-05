package io.audita.infrastructure.service;

import io.audita.application.port.SsoPort;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory SSO state store backed by ConcurrentHashMap.
 *
 * NOTE: This implementation is suitable for single-instance deployments only.
 * In a multi-instance deployment, in-memory state will not be shared across
 * instances, causing SSO callback failures when the callback reaches a
 * different instance than the one that initiated the authorization flow.
 *
 * For multi-instance HA, replace this with a Redis-backed or database-backed
 * implementation that all instances can access.
 */
@Component
public class InMemorySsoStateStore implements SsoStateStore {

    private final ConcurrentHashMap<String, SsoService.SsoState> pendingStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SsoService.FrontendExchangeState> pendingExchanges = new ConcurrentHashMap<>();

    @Override
    public void putState(String stateToken, String tenantSlug, OAuthProvider provider, long expiresAt) {
        pendingStates.put(stateToken, new SsoService.SsoState(tenantSlug, provider, expiresAt));
    }

    @Override
    public SsoService.SsoState validateAndRemoveState(String stateToken) {
        SsoService.SsoState state = pendingStates.remove(stateToken);
        if (state == null) {
            throw new DomainNotPermittedException("INVALID_STATE",
                    "Invalid or expired SSO state. Please start the sign-in process again.");
        }
        if (System.currentTimeMillis() > state.expiresAt()) {
            throw new DomainNotPermittedException("INVALID_STATE",
                    "SSO session expired. Please start the sign-in process again.");
        }
        return state;
    }

    @Override
    public void putExchange(String code, SsoPort.SsoResult result, long expiresAt) {
        pendingExchanges.put(code, new SsoService.FrontendExchangeState(result, expiresAt));
    }

    @Override
    public SsoService.FrontendExchangeState consumeExchange(String code) {
        return pendingExchanges.remove(code);
    }

    @Override
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        pendingStates.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        pendingExchanges.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }
}
