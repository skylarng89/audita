package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit log entry — no setters, no @PreUpdate.
 * The application DB role must not be granted UPDATE/DELETE on this table.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID actorId;

    // Denormalised: preserved even if the user is later deleted
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

    private Long chainIndex;

    private byte[] recordHash;

    private byte[] previousHash;

    protected AuditLogEntity() {}

    public AuditLogEntity(UUID actorId, String actorEmail, String actionType,
                          String entityType, UUID entityId,
                          Map<String, Object> payload, String ipAddress,
                          Long chainIndex, byte[] recordHash, byte[] previousHash) {
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.payload = payload;
        this.ipAddress = ipAddress;
        this.chainIndex = chainIndex;
        this.recordHash = recordHash;
        this.previousHash = previousHash;
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getActionType() { return actionType; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public Map<String, Object> getPayload() { return payload; }
    public String getIpAddress() { return ipAddress; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Long getChainIndex() { return chainIndex; }
    public byte[] getRecordHash() { return recordHash; }
    public byte[] getPreviousHash() { return previousHash; }
}
