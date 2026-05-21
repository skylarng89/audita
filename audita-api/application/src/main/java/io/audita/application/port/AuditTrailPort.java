package io.audita.application.port;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditTrailPort {

    Page<AuditLogEntry> query(String actorEmail,
                              String actionType,
                              String entityType,
                              LocalDate from,
                              LocalDate to,
                              Pageable pageable);

    List<AuditLogEntry> export(String actorEmail,
                               String actionType,
                               String entityType,
                               LocalDate from,
                               LocalDate to);

    record AuditLogEntry(
            UUID id,
            UUID actorId,
            String actorFullName,
            String actorEmail,
            String actionType,
            String entityType,
            UUID entityId,
            Map<String, Object> payload,
            String ipAddress,
            OffsetDateTime createdAt
    ) {
    }
}
