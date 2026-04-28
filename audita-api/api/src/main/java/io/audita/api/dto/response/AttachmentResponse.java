package io.audita.api.dto.response;

import io.audita.infrastructure.persistence.entity.AttachmentEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String fileName,
        String mimeType,
        long sizeBytes,
        String storagePath,
        UUID uploaderId,
        String uploaderName,
        OffsetDateTime createdAt
) {
    public static AttachmentResponse from(AttachmentEntity entity) {
        UUID uploaderId = entity.getUploader() != null ? entity.getUploader().getId() : null;
        String uploaderName = entity.getUploader() != null ? entity.getUploader().getFullName() : null;

        return new AttachmentResponse(
                entity.getId(),
                entity.getFileName(),
                entity.getMimeType(),
                entity.getSizeBytes(),
            null,
                uploaderId,
                uploaderName,
                entity.getCreatedAt()
        );
    }
}
