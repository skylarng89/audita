package io.audita.infrastructure.persistence.entity;

import io.audita.domain.exception.DomainException;
import io.audita.domain.model.ApproverStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cr_approvers")
public class CrApproverEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequestEntity changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private boolean isRequired = true;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApproverStatus status = ApproverStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private OffsetDateTime decidedAt;

    @Column(nullable = false)
    private boolean isAdHoc = false;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "is_sample", nullable = false)
    private boolean isSample = false;

    protected CrApproverEntity() {}

    public CrApproverEntity(ChangeRequestEntity changeRequest, UserEntity user,
                             boolean isRequired, int position, boolean isAdHoc) {
        this.changeRequest = changeRequest;
        this.user = user;
        this.isRequired = isRequired;
        this.position = position;
        this.isAdHoc = isAdHoc;
    }

    // ── Domain logic ─────────────────────────────────────────────────────────

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

    // ── Accessors ─────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public ChangeRequestEntity getChangeRequest() { return changeRequest; }
    public void setChangeRequest(ChangeRequestEntity changeRequest) { this.changeRequest = changeRequest; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { this.isRequired = required; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public ApproverStatus getStatus() { return status; }
    public void setStatus(ApproverStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }
    public boolean isAdHoc() { return isAdHoc; }
    public void setAdHoc(boolean adHoc) { this.isAdHoc = adHoc; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public boolean isSample() { return isSample; }
    public void setSample(boolean sample) { isSample = sample; }
}
