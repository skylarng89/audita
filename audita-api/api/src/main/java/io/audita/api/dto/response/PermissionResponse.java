package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.PermissionEntity;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String label
) {
    public static PermissionResponse from(PermissionEntity e) {
        return new PermissionResponse(e.getId(), e.getCode(), e.getLabel());
    }
}
