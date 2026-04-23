package io.audita.infrastructure.persistence.entity;

import io.audita.domain.model.ApproverStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cr_approvers")
@Getter
@Setter
@NoArgsConstructor
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

    public CrApproverEntity(ChangeRequestEntity changeRequest, UserEntity user,
                             boolean isRequired, int position, boolean isAdHoc) {
        this.changeRequest = changeRequest;
        this.user = user;
        this.isRequired = isRequired;
        this.position = position;
        this.isAdHoc = isAdHoc;
    }

    public void approve() {
        this.status = ApproverStatus.APPROVED;
        this.rejectionReason = null;
        this.decidedAt = OffsetDateTime.now();
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new io.audita.domain.exception.DomainException("Rejection reason is required.");
        }
        this.status = ApproverStatus.REJECTED;
        this.rejectionReason = reason;
        this.decidedAt = OffsetDateTime.now();
    }
}
