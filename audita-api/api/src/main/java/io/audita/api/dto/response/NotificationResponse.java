package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.NotificationEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String body,
        String link,
        boolean isRead,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getLink(),
                entity.isRead(),
                entity.getCreatedAt()
        );
    }
}
