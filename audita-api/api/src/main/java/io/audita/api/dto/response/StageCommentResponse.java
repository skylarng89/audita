package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.RequestDeploymentCommentEntity;
import io.audita.infrastructure.persistence.entity.RequestUatCommentEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StageCommentResponse(
        UUID id,
        UserResponse author,
        String body,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static StageCommentResponse from(RequestUatCommentEntity entity, UserEntity author) {
        return new StageCommentResponse(
                entity.getId(),
                author == null ? null : UserResponse.from(author),
                entity.getBody(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static StageCommentResponse from(RequestDeploymentCommentEntity entity, UserEntity author) {
        return new StageCommentResponse(
                entity.getId(),
                author == null ? null : UserResponse.from(author),
                entity.getBody(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
