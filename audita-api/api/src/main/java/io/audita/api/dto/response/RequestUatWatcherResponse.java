package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.RequestUatWatcherEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestUatWatcherResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String userFullName,
        OffsetDateTime createdAt
) {
    public static RequestUatWatcherResponse from(RequestUatWatcherEntity entity) {
        return new RequestUatWatcherResponse(
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                null,
                null,
                entity.getCreatedAt()
        );
    }

    public static RequestUatWatcherResponse from(RequestUatWatcherEntity entity, UserEntity user) {
        return new RequestUatWatcherResponse(
                entity.getId(),
                user != null ? user.getId() : (entity.getUser() != null ? entity.getUser().getId() : null),
                user != null ? user.getEmail() : null,
                user != null ? user.getFullName() : null,
                entity.getCreatedAt()
        );
    }
}
