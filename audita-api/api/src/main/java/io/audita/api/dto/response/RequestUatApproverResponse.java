package io.audita.api.dto.response;

import io.audita.domain.model.ApproverStatus;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestUatApproverResponse(
        UUID id,
        UUID uatId,
        UUID userId,
        boolean isRequired,
        int position,
        ApproverStatus status,
        String rejectionReason,
        OffsetDateTime decidedAt
) {
    public static RequestUatApproverResponse from(RequestUatApproverEntity entity) {
        return new RequestUatApproverResponse(
                entity.getId(),
                entity.getUatId(),
                entity.getUserId(),
                entity.isRequired(),
                entity.getPosition(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getDecidedAt()
        );
    }
}
