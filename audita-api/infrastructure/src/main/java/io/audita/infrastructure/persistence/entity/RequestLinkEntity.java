package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "request_links")
public class RequestLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id_a", nullable = false)
    private UUID requestIdA;

    @Column(name = "request_id_b", nullable = false)
    private UUID requestIdB;

    @Column(name = "linked_by")
    private UUID linkedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public RequestLinkEntity() {}

    public static UUID[] canonicalOrder(UUID a, UUID b) {
        if (a.equals(b)) {
            throw new IllegalArgumentException("Cannot link a request to itself");
        }
        if (a.toString().compareTo(b.toString()) < 0) {
            return new UUID[]{a, b};
        }
        return new UUID[]{b, a};
    }

    public UUID getId() { return id; }
    public UUID getRequestIdA() { return requestIdA; }
    public void setRequestIdA(UUID requestIdA) { this.requestIdA = requestIdA; }
    public UUID getRequestIdB() { return requestIdB; }
    public void setRequestIdB(UUID requestIdB) { this.requestIdB = requestIdB; }
    public UUID getLinkedBy() { return linkedBy; }
    public void setLinkedBy(UUID linkedBy) { this.linkedBy = linkedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
