package io.audita.api.dto.response;

import io.audita.domain.model.ApproverStatus;
import io.audita.infrastructure.persistence.entity.RequestDeploymentApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestDeploymentEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RequestDeploymentResponse(
        UUID id,
        UUID requestId,
        UUID uatId,
        String status,
        UUID createdBy,
        OffsetDateTime promotedAt,
        OffsetDateTime completedAt,
        List<DeploymentApproverResponse> approvers
) {
    public static RequestDeploymentResponse from(RequestDeploymentEntity entity) {
        return new RequestDeploymentResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getUatId(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getPromotedAt(),
                entity.getCompletedAt(),
                List.of()
        );
    }

    public static RequestDeploymentResponse from(
            RequestDeploymentEntity entity,
            List<RequestDeploymentApproverEntity> approvers,
            Map<UUID, UserEntity> users) {
        return new RequestDeploymentResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getUatId(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getPromotedAt(),
                entity.getCompletedAt(),
                approvers.stream()
                        .map(a -> DeploymentApproverResponse.from(a, users.get(a.getUserId())))
                        .toList()
        );
    }

    public record DeploymentApproverResponse(
            UUID id,
            UUID deploymentId,
            UUID userId,
            String userFullName,
            String userEmail,
            boolean isRequired,
            ApproverStatus status,
            OffsetDateTime decidedAt,
            String rejectionReason
    ) {
        public static DeploymentApproverResponse from(RequestDeploymentApproverEntity entity) {
            return new DeploymentApproverResponse(
                    entity.getId(),
                    entity.getDeploymentId(),
                    entity.getUserId(),
                    null,
                    null,
                    entity.isRequired(),
                    entity.getStatus(),
                    entity.getDecidedAt(),
                    entity.getRejectionReason()
            );
        }

        public static DeploymentApproverResponse from(RequestDeploymentApproverEntity entity, UserEntity user) {
            return new DeploymentApproverResponse(
                    entity.getId(),
                    entity.getDeploymentId(),
                    entity.getUserId(),
                    user != null ? user.getFullName() : null,
                    user != null ? user.getEmail() : null,
                    entity.isRequired(),
                    entity.getStatus(),
                    entity.getDecidedAt(),
                    entity.getRejectionReason()
            );
        }
    }
}
