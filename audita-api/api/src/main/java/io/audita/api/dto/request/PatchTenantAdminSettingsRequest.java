package io.audita.api.dto.request;

import io.audita.domain.model.ApprovalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PatchTenantAdminSettingsRequest(
        @NotNull @Valid WorkflowDefaults workflowDefaults,
        @NotNull @Valid SlaDefaults slaDefaults
) {
    public record WorkflowDefaults(
            @NotNull ApprovalType approvalTypeDefault,
            @NotNull Boolean requireDefaultApprovers
    ) {
    }

    public record SlaDefaults(
            @NotNull @Min(1) @Max(720) Integer lowHours,
            @NotNull @Min(1) @Max(720) Integer mediumHours,
            @NotNull @Min(1) @Max(720) Integer highHours,
            @NotNull @Min(1) @Max(720) Integer criticalHours,
            @NotNull @Min(1) @Max(168) Integer warningBeforeHours
    ) {
    }
}