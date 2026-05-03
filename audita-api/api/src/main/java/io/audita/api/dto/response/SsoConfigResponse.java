package io.audita.api.dto.response;

import io.audita.domain.model.OAuthProvider;
import io.audita.infrastructure.persistence.entity.TenantSsoConfigEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SsoConfigResponse(
        UUID id,
        OAuthProvider provider,
        String clientId,
        String msTenantId,
        boolean enabled,
        OffsetDateTime createdAt
) {
    public static SsoConfigResponse from(TenantSsoConfigEntity e) {
        // Never expose the client secret in the response
        return new SsoConfigResponse(e.getId(), e.getProvider(), e.getClientId(),
                e.getMsTenantId(), e.isEnabled(), e.getCreatedAt());
    }
}
