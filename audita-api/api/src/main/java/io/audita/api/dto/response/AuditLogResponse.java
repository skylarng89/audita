package io.audita.api.dto.response;

import io.audita.application.port.AuditTrailPort;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String actionType,
        String entityType,
        UUID entityId,
        Map<String, Object> payload,
        String ipAddress,
        OffsetDateTime createdAt
) {
    public static AuditLogResponse from(AuditTrailPort.AuditLogEntry entry) {
        return new AuditLogResponse(
                entry.id(),
                entry.actorId(),
                entry.actorEmail(),
                entry.actionType(),
                entry.entityType(),
                entry.entityId(),
                entry.payload(),
                entry.ipAddress(),
                entry.createdAt()
        );
    }
}
