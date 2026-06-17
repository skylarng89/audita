package io.audita.infrastructure.persistence.entity;

import io.audita.domain.exception.InvalidStateTransitionException;
import io.audita.domain.model.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "change_requests")
public class ChangeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeRequestStatus status = ChangeRequestStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalType approvalType;

    @Column(nullable = false)
    private boolean approvalLocked = false;

    private OffsetDateTime scheduledStart;
    private OffsetDateTime scheduledEnd;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    private String[] affectedSystems = new String[0];

    private OffsetDateTime slaDeadline;

    @Column(nullable = false)
    private boolean slaBreached = false;

    @Column(name = "is_sample", nullable = false)
    private boolean isSample = false;

    @Column(name = "display_id")
    private String displayId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ChangeRequestStatus approvalStatus = ChangeRequestStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false)
    private CompletionStatus completionStatus = CompletionStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_mode", nullable = false)
    private RequestWorkflowMode workflowMode = RequestWorkflowMode.APPROVAL_ONLY;

    @Column(name = "request_department_id")
    private UUID requestDepartmentId;

    @Column(name = "destination_department_id")
    private UUID destinationDepartmentId;

    @Column(name = "request_group_id")
    private UUID requestGroupId;

    @Column(name = "destination_group_id")
    private UUID destinationGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<CrApproverEntity> approvers = new ArrayList<>();

    public ChangeRequestEntity() {}

    @PreUpdate
    public void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    // ── Domain logic ─────────────────────────────────────────────────────────

    public void submit() {
        if (this.status != ChangeRequestStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                "Only DRAFT change requests can be submitted. Current status: " + this.status);
        }
        this.status = ChangeRequestStatus.PENDING_APPROVAL;
        this.approvalStatus = ChangeRequestStatus.PENDING_APPROVAL;
    }

    public void cancel() {
        if (this.status.isClosed()) {
            throw new InvalidStateTransitionException(
                "Cannot cancel a closed change request. Current status: " + this.status);
        }
        this.status = ChangeRequestStatus.CANCELLED;
        this.approvalStatus = ChangeRequestStatus.CANCELLED;
    }

    public void markApprovalLocked() { this.approvalLocked = true; }
    public void markSlaBreached() { this.slaBreached = true; }

    /**
     * Re-evaluates and applies closure state after every approver decision (WF-09, WF-10, WF-12).
     */
    public void evaluateApprovalClosure() {
        if (this.status != ChangeRequestStatus.PENDING_APPROVAL) {
            return;
        }

        List<CrApproverEntity> required = approvers.stream()
                .filter(CrApproverEntity::isRequired)
                .toList();

        if (required.isEmpty()) {
            required = approvers;
            if (required.isEmpty()) {
                return;
            }
        }

        boolean allRequiredApproved = required.stream()
                .allMatch(a -> a.getStatus() == ApproverStatus.APPROVED);
        if (allRequiredApproved) {
            this.status = ChangeRequestStatus.APPROVED;
            this.approvalStatus = ChangeRequestStatus.APPROVED;
            return;
        }

        // Single required approver who rejected → immediately closed
        if (required.size() == 1 && required.get(0).getStatus() == ApproverStatus.REJECTED) {
            this.status = ChangeRequestStatus.REJECTED;
            this.approvalStatus = ChangeRequestStatus.REJECTED;
            return;
        }

        // Multiple required approvers: rejected only when ALL required have rejected
        boolean allRequiredRejected = required.stream()
                .allMatch(a -> a.getStatus() == ApproverStatus.REJECTED);
        if (allRequiredRejected) {
            this.status = ChangeRequestStatus.REJECTED;
            this.approvalStatus = ChangeRequestStatus.REJECTED;
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public ChangeRequestStatus getStatus() { return status; }
    public void setStatus(ChangeRequestStatus status) { this.status = status; }
    public ApprovalType getApprovalType() { return approvalType; }
    public void setApprovalType(ApprovalType approvalType) { this.approvalType = approvalType; }
    public boolean isApprovalLocked() { return approvalLocked; }
    public void setApprovalLocked(boolean approvalLocked) { this.approvalLocked = approvalLocked; }
    public OffsetDateTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(OffsetDateTime scheduledStart) { this.scheduledStart = scheduledStart; }
    public OffsetDateTime getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(OffsetDateTime scheduledEnd) { this.scheduledEnd = scheduledEnd; }
    public String[] getAffectedSystems() { return affectedSystems; }
    public void setAffectedSystems(String[] affectedSystems) { this.affectedSystems = affectedSystems; }
    public OffsetDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(OffsetDateTime slaDeadline) { this.slaDeadline = slaDeadline; }
    public boolean isSlaBreached() { return slaBreached; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }
    public UserEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserEntity createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<CrApproverEntity> getApprovers() { return approvers; }
    public boolean isSample() { return isSample; }
    public void setSample(boolean sample) { isSample = sample; }
    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }
    public ChangeRequestStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ChangeRequestStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public CompletionStatus getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(CompletionStatus completionStatus) { this.completionStatus = completionStatus; }
    public RequestWorkflowMode getWorkflowMode() { return workflowMode; }
    public void setWorkflowMode(RequestWorkflowMode workflowMode) { this.workflowMode = workflowMode; }
    public UUID getRequestDepartmentId() { return requestDepartmentId; }
    public void setRequestDepartmentId(UUID requestDepartmentId) { this.requestDepartmentId = requestDepartmentId; }
    public UUID getDestinationDepartmentId() { return destinationDepartmentId; }
    public void setDestinationDepartmentId(UUID destinationDepartmentId) { this.destinationDepartmentId = destinationDepartmentId; }
    public UUID getRequestGroupId() { return requestGroupId; }
    public void setRequestGroupId(UUID requestGroupId) { this.requestGroupId = requestGroupId; }
    public UUID getDestinationGroupId() { return destinationGroupId; }
    public void setDestinationGroupId(UUID destinationGroupId) { this.destinationGroupId = destinationGroupId; }
}
