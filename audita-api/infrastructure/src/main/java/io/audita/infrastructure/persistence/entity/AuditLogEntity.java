package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit log — no setters, no @PreUpdate.
 * Insert-only. The app DB role should not be granted UPDATE/DELETE on this table.
 */
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID actorId;

    // Denormalised — preserved even if the user is later deleted
    private String actorEmail;

    @Column(nullable = false)
    private String actionType;

    private String entityType;
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public AuditLogEntity(UUID actorId, String actorEmail, String actionType,
                           String entityType, UUID entityId,
                           Map<String, Object> payload, String ipAddress) {
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.payload = payload;
        this.ipAddress = ipAddress;
    }
}
