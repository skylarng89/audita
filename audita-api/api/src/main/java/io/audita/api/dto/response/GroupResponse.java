package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.GroupEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        boolean isActive,
        int displayOrder,
        int memberCount,
        OffsetDateTime createdAt
) {
    public static GroupResponse from(GroupEntity e) {
        return new GroupResponse(e.getId(), e.getName(), e.getDescription(), e.isActive(), e.getDisplayOrder(), e.getMemberCount(), e.getCreatedAt());
    }

    public static GroupResponse from(GroupEntity e, int memberCount) {
        return new GroupResponse(e.getId(), e.getName(), e.getDescription(), e.isActive(), e.getDisplayOrder(), memberCount, e.getCreatedAt());
    }
}
