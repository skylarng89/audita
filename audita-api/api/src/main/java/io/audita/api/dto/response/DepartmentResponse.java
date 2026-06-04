package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.DepartmentEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        String code,
        boolean isActive,
        int displayOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DepartmentResponse from(DepartmentEntity entity) {
        return new DepartmentResponse(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.isActive(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
