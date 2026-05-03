package io.audita.api.dto.response;

import io.audita.domain.model.TenantStatus;
import io.audita.infrastructure.persistence.entity.TenantEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TenantResponse from(TenantEntity e) {
        return new TenantResponse(e.getId(), e.getName(), e.getSlug(),
                e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
