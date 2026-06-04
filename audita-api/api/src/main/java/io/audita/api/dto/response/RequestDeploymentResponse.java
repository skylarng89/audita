package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestDeploymentResponse(
        UUID id,
        UUID requestId,
        UUID uatId,
        String status,
        UUID createdBy,
        OffsetDateTime promotedAt,
        OffsetDateTime completedAt
) {
    public static RequestDeploymentResponse from(RequestDeploymentEntity entity) {
        return new RequestDeploymentResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getUatId(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getPromotedAt(),
                entity.getCompletedAt()
        );
    }
}
