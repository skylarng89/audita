package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.RequestUatApproverEntity;
import io.audita.infrastructure.persistence.entity.RequestUatEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RequestUatResponse(
        UUID id,
        UUID requestId,
        String title,
        String details,
        String status,
        boolean readOnly,
        boolean requesterSignedOff,
        UUID createdBy,
        String createdByFullName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RequestUatApproverResponse> approvers
) {
    public static RequestUatResponse from(RequestUatEntity entity) {
        return new RequestUatResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getTitle(),
                entity.getDetails(),
                entity.getStatus(),
                entity.isReadOnly(),
                entity.isRequesterSignedOff(),
                entity.getCreatedBy(),
                null,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                List.of()
        );
    }

    public static RequestUatResponse from(RequestUatEntity entity, String createdByFullName,
            List<RequestUatApproverResponse> approvers) {
        return new RequestUatResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getTitle(),
                entity.getDetails(),
                entity.getStatus(),
                entity.isReadOnly(),
                entity.isRequesterSignedOff(),
                entity.getCreatedBy(),
                createdByFullName,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                approvers
        );
    }
}
