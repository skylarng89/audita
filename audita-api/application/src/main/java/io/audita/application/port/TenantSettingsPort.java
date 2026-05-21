package io.audita.application.port;

import io.audita.domain.model.ApprovalType;

import java.util.List;
import java.util.UUID;

public interface TenantSettingsPort {

    TenantSettings getTenantSettings(String tenantSlug);

    void updateProfile(String tenantSlug, ProfileUpdate profile);

    void updateWorkflowDefaults(String tenantSlug, WorkflowDefaults workflowDefaults);

    void updateSlaDefaults(String tenantSlug, SlaDefaults slaDefaults);

    void updateAuditDefaults(String tenantSlug, AuditDefaults auditDefaults);

    void updateAutoApproverDefaults(String tenantSlug, AutoApproverDefaults autoApproverDefaults);

    record TenantProfile(
            String name,
            String slug,
            String primaryContactEmail,
            String timezone,
            String status) {
    }

    record ProfileUpdate(
            String name,
            String primaryContactEmail,
            String timezone) {
    }

    record WorkflowDefaults(
            ApprovalType approvalTypeDefault,
            boolean requireDefaultApprovers) {
    }

    record SlaDefaults(
            int lowHours,
            int mediumHours,
            int highHours,
            int criticalHours,
            int warningBeforeHours) {
    }

    record AutoApproverDefaults(
            List<UUID> userIds,
            List<UUID> groupIds) {
    }

    record AuditDefaults(
            int exportLinkExpiryHours) {
    }

    record TenantSettings(
            TenantProfile profile,
            WorkflowDefaults workflowDefaults,
            SlaDefaults slaDefaults,
            AutoApproverDefaults autoApproverDefaults,
            AuditDefaults auditDefaults) {
    }
}
