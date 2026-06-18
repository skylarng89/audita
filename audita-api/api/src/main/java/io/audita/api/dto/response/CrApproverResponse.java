package io.audita.api.dto.response;

import io.audita.domain.model.ApproverStatus;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CrApproverResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String userFullName,
        String userRole,
        int position,
        ApproverStatus status,
        String rejectionReason,
        OffsetDateTime decidedAt
) {
    public static CrApproverResponse from(CrApproverEntity entity) {
        String roleName = entity.getUser().getRoles().stream()
                .map(io.audita.infrastructure.persistence.entity.RoleEntity::getName)
                .findFirst()
                .orElse(null);
        return new CrApproverResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getEmail(),
                entity.getUser().getFullName(),
                roleName,
                entity.getPosition(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getDecidedAt()
        );
    }
}
