package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.TenantAllowedDomainEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DomainResponse(
        UUID id,
        String domain,
        OffsetDateTime createdAt
) {
    public static DomainResponse from(TenantAllowedDomainEntity e) {
        return new DomainResponse(e.getId(), e.getDomain(), e.getCreatedAt());
    }
}
