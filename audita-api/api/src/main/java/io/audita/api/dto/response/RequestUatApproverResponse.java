package io.audita.api.dto.response;

import io.audita.domain.model.ApproverStatus;
import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestUatApproverResponse(
        UUID id,
        UUID uatId,
        UUID userId,
        String userFullName,
        String userEmail,
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
                null,
                null,
                entity.isRequired(),
                entity.getPosition(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getDecidedAt()
        );
    }

    public static RequestUatApproverResponse from(RequestUatApproverEntity entity, UserEntity user) {
        return new RequestUatApproverResponse(
                entity.getId(),
                entity.getUatId(),
                entity.getUserId(),
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                entity.isRequired(),
                entity.getPosition(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getDecidedAt()
        );
    }
}
