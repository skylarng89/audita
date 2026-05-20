package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "operation", nullable = false, length = 100)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected IdempotencyKeyEntity() {
    }

    public IdempotencyKeyEntity(UUID userId,
                                String operation,
                                String idempotencyKey,
                                UUID resourceId,
                                OffsetDateTime expiresAt) {
        this.userId = userId;
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.resourceId = resourceId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOperation() {
        return operation;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
