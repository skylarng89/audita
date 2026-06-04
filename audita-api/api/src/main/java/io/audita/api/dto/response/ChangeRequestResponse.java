package io.audita.api.dto.response;

import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.ChangeRequestStatus;
import io.audita.domain.model.CompletionStatus;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.domain.model.RiskLevel;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record ChangeRequestResponse(
        UUID id,
        String title,
        String description,
        Priority priority,
        RiskLevel riskLevel,
        String category,
        ChangeRequestStatus status,
        ApprovalType approvalType,
        boolean approvalLocked,
        OffsetDateTime scheduledStart,
        OffsetDateTime scheduledEnd,
        List<String> affectedSystems,
        OffsetDateTime slaDeadline,
        boolean slaBreached,
        UUID createdBy,
        String createdByEmail,
        String createdByFullName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String displayId,
        ChangeRequestStatus approvalStatus,
        CompletionStatus completionStatus,
        RequestWorkflowMode workflowMode,
        UUID requestDepartmentId,
        UUID destinationDepartmentId
) {
    public static ChangeRequestResponse from(ChangeRequestEntity entity) {
        UUID createdById = entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null;
        String createdByEmail = entity.getCreatedBy() != null ? entity.getCreatedBy().getEmail() : null;
        String createdByFullName = entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null;
        List<String> systems = entity.getAffectedSystems() == null
                ? List.of()
                : Arrays.asList(entity.getAffectedSystems());

        return new ChangeRequestResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getRiskLevel(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getApprovalType(),
                entity.isApprovalLocked(),
                entity.getScheduledStart(),
                entity.getScheduledEnd(),
                systems,
                entity.getSlaDeadline(),
                entity.isSlaBreached(),
                createdById,
                createdByEmail,
                createdByFullName,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDisplayId(),
                entity.getApprovalStatus(),
                entity.getCompletionStatus(),
                entity.getWorkflowMode(),
                entity.getRequestDepartmentId(),
                entity.getDestinationDepartmentId()
        );
    }
}