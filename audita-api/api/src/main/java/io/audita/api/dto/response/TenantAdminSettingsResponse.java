package io.audita.api.dto.response;

import io.audita.domain.model.ApprovalType;

import java.util.List;
import java.util.UUID;

public record TenantAdminSettingsResponse(
                OrganizationProfile profile,
                FeatureFlags featureFlags,
                SecurityDefaults securityDefaults,
                WorkflowDefaults workflowDefaults,
                SlaDefaults slaDefaults,
                AutoApproverDefaults autoApproverDefaults,
                AuditDefaults auditDefaults,
                boolean sampleDataImported) {
        public record OrganizationProfile(
                        String name,
                        String slug,
                        String subdomain,
                        String primaryContactEmail,
                        String timezone,
                        String status) {
        }

        public record FeatureFlags(
                        boolean policyBreachDigests,
                        boolean automatedReminders,
                        boolean conditionalEscalation) {
        }

        public record SecurityDefaults(
                        Integer sessionTimeoutMinutes,
                        String mfaPolicy,
                        String passwordPolicy) {
        }

        public record WorkflowDefaults(
                        ApprovalType approvalTypeDefault,
                        boolean requireDefaultApprovers) {
        }

        public record SlaDefaults(
                        int lowHours,
                        int mediumHours,
                        int highHours,
                        int criticalHours,
                        int warningBeforeHours) {
        }

        public record AutoApproverDefaults(
                        List<UUID> userIds,
                        List<UUID> groupIds) {
        }

        public record AuditDefaults(
                        int exportLinkExpiryHours) {
        }
}
