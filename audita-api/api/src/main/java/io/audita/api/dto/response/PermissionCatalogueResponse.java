package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.PermissionEntity;

import java.util.List;

public record PermissionCatalogueResponse(
        List<PermissionEntry> permissions
) {
    public record PermissionEntry(String code, String label) {}

    public static PermissionCatalogueResponse from(List<PermissionEntity> permissions) {
        List<PermissionEntry> entries = permissions.stream()
                .map(p -> new PermissionEntry(p.getCode(), p.getLabel()))
                .toList();
        return new PermissionCatalogueResponse(entries);
    }
}
