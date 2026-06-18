package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestDeploymentResponse(
        UUID id,
        UUID requestId,
        UUID uatId,
        String status,
        UserSummary assignee,
        String createdByFullName,
        OffsetDateTime promotedAt,
        OffsetDateTime completedAt
) {
    public static RequestDeploymentResponse from(RequestDeploymentEntity entity,
            UserEntity assignee, String createdByFullName) {
        return new RequestDeploymentResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getUatId(),
                entity.getStatus(),
                UserSummary.from(assignee),
                createdByFullName,
                entity.getPromotedAt(),
                entity.getCompletedAt()
        );
    }

    public static RequestDeploymentResponse from(RequestDeploymentEntity entity) {
        return from(entity, null, null);
    }

    public record UserSummary(
            UUID id,
            String email,
            String fullName
    ) {
        public static UserSummary from(UserEntity user) {
            if (user == null) {
                return null;
            }
            return new UserSummary(user.getId(), user.getEmail(), user.getFullName());
        }
    }
}
