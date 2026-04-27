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
        boolean isRequired,
        int position,
        ApproverStatus status,
        String rejectionReason,
        OffsetDateTime decidedAt,
        boolean isAdHoc
) {
    public static CrApproverResponse from(CrApproverEntity entity) {
        return new CrApproverResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getEmail(),
                entity.getUser().getFullName(),
                entity.isRequired(),
                entity.getPosition(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getDecidedAt(),
                entity.isAdHoc()
        );
    }
}
