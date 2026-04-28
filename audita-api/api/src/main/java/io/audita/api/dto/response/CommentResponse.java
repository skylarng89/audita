package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.CommentEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UserResponse author,
        String body,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CommentResponse from(CommentEntity entity) {
        return new CommentResponse(
                entity.getId(),
                entity.getAuthor() == null ? null : UserResponse.from(entity.getAuthor()),
                entity.getBody(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
