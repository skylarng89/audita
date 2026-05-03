package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.GroupEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdAt
) {
    public static GroupResponse from(GroupEntity e) {
        return new GroupResponse(e.getId(), e.getName(), e.getDescription(), e.getCreatedAt());
    }
}
