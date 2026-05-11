package io.audita.application.port;

import io.audita.domain.model.ApprovalType;

public interface TenantSettingsPort {

    TenantSettings getTenantSettings(String tenantSlug);

    void updateWorkflowDefaults(String tenantSlug, WorkflowDefaults workflowDefaults);

    void updateSlaDefaults(String tenantSlug, SlaDefaults slaDefaults);

    record TenantProfile(
            String name,
            String slug,
            String status
    ) {
    }

    record WorkflowDefaults(
        ApprovalType approvalTypeDefault,
        boolean requireDefaultApprovers
    ) {
    }

    record SlaDefaults(
        int lowHours,
        int mediumHours,
        int highHours,
        int criticalHours,
        int warningBeforeHours
    ) {
    }

    record TenantSettings(
        TenantProfile profile,
        WorkflowDefaults workflowDefaults,
        SlaDefaults slaDefaults
    ) {
    }
}