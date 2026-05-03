package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.RoleEntity;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        boolean isSystem,
        List<PermissionResponse> permissions
) {
    public static RoleResponse from(RoleEntity e) {
        List<PermissionResponse> perms = e.getPermissions().stream()
                .map(PermissionResponse::from)
                .toList();
        return new RoleResponse(e.getId(), e.getName(), e.getDescription(), e.isSystem(), perms);
    }
}
