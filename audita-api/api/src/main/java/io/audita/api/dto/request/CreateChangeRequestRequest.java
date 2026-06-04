package io.audita.api.dto.request;

import io.audita.domain.model.ApprovalType;
import io.audita.domain.model.Priority;
import io.audita.domain.model.RequestWorkflowMode;
import io.audita.domain.model.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateChangeRequestRequest(
                @NotBlank @Size(max = 500) String title,
                String description,
                @NotNull Priority priority,
                @NotNull RiskLevel riskLevel,
                @Size(max = 255) String category,
                ApprovalType approvalType,
                OffsetDateTime scheduledStart,
                OffsetDateTime scheduledEnd,
                List<String> affectedSystems,
                RequestWorkflowMode workflowMode,
                UUID requestDepartmentId,
                UUID destinationDepartmentId) {
}