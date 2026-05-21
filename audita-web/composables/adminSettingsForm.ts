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

export type AuditDefaults = {
  exportLinkExpiryHours: number;
};

export type OrganizationProfile = {
  name: string;
  primaryContactEmail: string;
  timezone: string;
};

export type SettingsPatchPayload = {
  profile: OrganizationProfile;
  workflowDefaults: WorkflowDefaults;
  slaDefaults: SlaDefaults;
  autoApproverDefaults: AutoApproverDefaults;
  auditDefaults: AuditDefaults;
};

export function createSettingsSnapshot(
  profile: OrganizationProfile,
  workflowDefaults: WorkflowDefaults,
  slaDefaults: SlaDefaults,
  autoApproverDefaults: AutoApproverDefaults,
  auditDefaults: AuditDefaults,
): string {
  return JSON.stringify({
    profile,
    workflowDefaults,
    slaDefaults,
    auditDefaults,
    autoApproverDefaults: {
      userIds: [...autoApproverDefaults.userIds].sort(),
      groupIds: [...autoApproverDefaults.groupIds].sort(),
    },
  });
}

export function isSettingsDirty(
  snapshot: string,
  profile: OrganizationProfile,
  workflowDefaults: WorkflowDefaults,
  slaDefaults: SlaDefaults,
  autoApproverDefaults: AutoApproverDefaults,
  auditDefaults: AuditDefaults,
): boolean {
  return (
    snapshot !==
    createSettingsSnapshot(
      profile,
      workflowDefaults,
      slaDefaults,
      autoApproverDefaults,
      auditDefaults,
    )
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
  profile: OrganizationProfile,
  workflowDefaults: WorkflowDefaults,
  slaDefaults: SlaDefaults,
  autoApproverDefaults: AutoApproverDefaults,
  auditDefaults: AuditDefaults,
): SettingsPatchPayload {
  return {
    profile: {
      name: profile.name,
      primaryContactEmail: profile.primaryContactEmail,
      timezone: profile.timezone,
    },
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
    auditDefaults: {
      exportLinkExpiryHours: auditDefaults.exportLinkExpiryHours,
    },
  };
}
