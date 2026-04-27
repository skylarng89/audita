package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ActivityStreamResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String actorFullName,
        String actionType,
        Map<String, Object> payload,
        OffsetDateTime createdAt
) {
    public static ActivityStreamResponse from(ActivityStreamEntity entity) {
        UUID actorId = entity.getActor() != null ? entity.getActor().getId() : null;
        String actorEmail = entity.getActor() != null ? entity.getActor().getEmail() : null;
        String actorFullName = entity.getActor() != null ? entity.getActor().getFullName() : null;

        return new ActivityStreamResponse(
                entity.getId(),
                actorId,
                actorEmail,
                actorFullName,
                entity.getActionType(),
                entity.getPayload(),
                entity.getCreatedAt()
        );
    }
}
