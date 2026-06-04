package io.audita.api.dto.response;

import io.audita.domain.model.ChangeRequestStatus;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;

import java.util.UUID;

public record RequestLinkSearchResponse(
        UUID id,
        String displayId,
        String title,
        ChangeRequestStatus status
) {
    public static RequestLinkSearchResponse from(ChangeRequestEntity entity) {
        return new RequestLinkSearchResponse(
                entity.getId(),
                entity.getDisplayId(),
                entity.getTitle(),
                entity.getStatus()
        );
    }
}
