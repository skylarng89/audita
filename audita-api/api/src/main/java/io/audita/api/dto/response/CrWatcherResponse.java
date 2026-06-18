package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.CrWatcherEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CrWatcherResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String userFullName,
        OffsetDateTime createdAt
) {
    public static CrWatcherResponse from(CrWatcherEntity entity) {
        return new CrWatcherResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getEmail(),
                entity.getUser().getFullName(),
                entity.getCreatedAt()
        );
    }
}
