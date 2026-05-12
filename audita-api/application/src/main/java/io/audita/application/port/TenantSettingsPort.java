package io.audita.application.port;

import io.audita.domain.model.ApprovalType;

import java.util.List;
import java.util.UUID;

public interface TenantSettingsPort {

    TenantSettings getTenantSettings(String tenantSlug);

    void updateWorkflowDefaults(String tenantSlug, WorkflowDefaults workflowDefaults);

    void updateSlaDefaults(String tenantSlug, SlaDefaults slaDefaults);

    void updateAutoApproverDefaults(String tenantSlug, AutoApproverDefaults autoApproverDefaults);

    record TenantProfile(
            String name,
            String slug,
            String status) {
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

    record TenantSettings(
            TenantProfile profile,
            WorkflowDefaults workflowDefaults,
            SlaDefaults slaDefaults,
            AutoApproverDefaults autoApproverDefaults) {
    }
}