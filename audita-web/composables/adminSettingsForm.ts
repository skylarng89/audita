export type WorkflowDefaults = {
  approvalTypeDefault: "LINEAR" | "NON_LINEAR";
  requireDefaultApprovers: boolean;
};

export type SlaDefaults = {
  lowHours: number;
  mediumHours: number;
  highHours: number;
  criticalHours: number;
  warningBeforeHours: number;
};

export type AutoApproverDefaults = {
  userIds: string[];
  groupIds: string[];
};

export type SettingsPatchPayload = {
  workflowDefaults: WorkflowDefaults;
  slaDefaults: SlaDefaults;
  autoApproverDefaults: AutoApproverDefaults;
};

export function createSettingsSnapshot(
  workflowDefaults: WorkflowDefaults,
  slaDefaults: SlaDefaults,
  autoApproverDefaults: AutoApproverDefaults,
): string {
  return JSON.stringify({
    workflowDefaults,
    slaDefaults,
    autoApproverDefaults: {
      userIds: [...autoApproverDefaults.userIds].sort(),
      groupIds: [...autoApproverDefaults.groupIds].sort(),
    },
  });
}

export function isSettingsDirty(
  snapshot: string,
  workflowDefaults: WorkflowDefaults,
  slaDefaults: SlaDefaults,
  autoApproverDefaults: AutoApproverDefaults,
): boolean {
  return (
    snapshot !==
    createSettingsSnapshot(workflowDefaults, slaDefaults, autoApproverDefaults)
  );
}

export function validateSlaDefaults(slaDefaults: SlaDefaults): string | null {
  const minDeadline = Math.min(
    slaDefaults.lowHours,
    slaDefaults.mediumHours,
    slaDefaults.highHours,
    slaDefaults.criticalHours,
  );

  if (slaDefaults.warningBeforeHours >= minDeadline) {
    return "Warning hours must be less than all SLA deadline values.";
  }
  return null;
}

export function buildSettingsPatchPayload(
  workflowDefaults: WorkflowDefaults,
  slaDefaults: SlaDefaults,
  autoApproverDefaults: AutoApproverDefaults,
): SettingsPatchPayload {
  return {
    workflowDefaults: {
      approvalTypeDefault: workflowDefaults.approvalTypeDefault,
      requireDefaultApprovers: workflowDefaults.requireDefaultApprovers,
    },
    slaDefaults: {
      lowHours: slaDefaults.lowHours,
      mediumHours: slaDefaults.mediumHours,
      highHours: slaDefaults.highHours,
      criticalHours: slaDefaults.criticalHours,
      warningBeforeHours: slaDefaults.warningBeforeHours,
    },
    autoApproverDefaults: {
      userIds: autoApproverDefaults.userIds,
      groupIds: autoApproverDefaults.groupIds,
    },
  };
}
