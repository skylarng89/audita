package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.RequestUatEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestUatResponse(
        UUID id,
        UUID requestId,
        String title,
        String details,
        String status,
        boolean readOnly,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static RequestUatResponse from(RequestUatEntity entity) {
        return new RequestUatResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getTitle(),
                entity.getDetails(),
                entity.getStatus(),
                entity.isReadOnly(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
