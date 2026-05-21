package io.audita.api.dto.request;

import io.audita.domain.model.ApprovalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PatchTenantAdminSettingsRequest(
                @NotNull @Valid OrganizationProfile profile,
                @NotNull @Valid WorkflowDefaults workflowDefaults,
                @NotNull @Valid SlaDefaults slaDefaults,
                @NotNull @Valid AutoApproverDefaults autoApproverDefaults,
                @NotNull @Valid AuditDefaults auditDefaults) {
        public record OrganizationProfile(
                        @NotNull String name,
                        String primaryContactEmail,
                        @NotNull String timezone) {
        }

        public record WorkflowDefaults(
                        @NotNull ApprovalType approvalTypeDefault,
                        @NotNull Boolean requireDefaultApprovers) {
        }

        public record SlaDefaults(
                        @NotNull @Min(1) @Max(720) Integer lowHours,
                        @NotNull @Min(1) @Max(720) Integer mediumHours,
                        @NotNull @Min(1) @Max(720) Integer highHours,
                        @NotNull @Min(1) @Max(720) Integer criticalHours,
                        @NotNull @Min(1) @Max(168) Integer warningBeforeHours) {
        }

        public record AutoApproverDefaults(
                        @NotNull List<UUID> userIds,
                        @NotNull List<UUID> groupIds) {
        }

        public record AuditDefaults(
                        @NotNull @Min(1) @Max(168) Integer exportLinkExpiryHours) {
        }
}
