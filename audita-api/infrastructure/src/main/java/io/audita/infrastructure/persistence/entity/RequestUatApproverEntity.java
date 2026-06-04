package io.audita.infrastructure.persistence.entity;

import io.audita.domain.exception.DomainException;
import io.audita.domain.model.ApproverStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "request_uat_approvers")
public class RequestUatApproverEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "uat_id", nullable = false)
    private UUID uatId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired = true;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApproverStatus status = ApproverStatus.PENDING;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    public RequestUatApproverEntity() {}

    public void approve() {
        this.status = ApproverStatus.APPROVED;
        this.rejectionReason = null;
        this.decidedAt = OffsetDateTime.now();
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException("Rejection reason is required.");
        }
        this.status = ApproverStatus.REJECTED;
        this.rejectionReason = reason;
        this.decidedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUatId() { return uatId; }
    public void setUatId(UUID uatId) { this.uatId = uatId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { this.isRequired = required; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public ApproverStatus getStatus() { return status; }
    public void setStatus(ApproverStatus status) { this.status = status; }
    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
