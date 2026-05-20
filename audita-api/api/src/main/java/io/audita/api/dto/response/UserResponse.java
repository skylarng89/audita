package io.audita.api.dto.response;

import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.UserEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UUID roleId,
        String roleName,
        List<RoleSummary> roles,
        UserStatus status,
        OffsetDateTime createdAt) {
    public static UserResponse from(UserEntity e) {
        UUID roleId = e.getRole() != null ? e.getRole().getId() : null;
        String roleName = e.getRole() != null ? e.getRole().getName() : null;
        List<RoleSummary> roles = e.getRoles().stream()
                .map(role -> new RoleSummary(role.getId(), role.getName()))
                .toList();
        return new UserResponse(e.getId(), e.getEmail(), e.getFullName(),
                roleId, roleName, roles, e.getStatus(), e.getCreatedAt());
    }

    public record RoleSummary(UUID id, String name) {
    }
}
