import {
  buildSettingsPatchPayload,
  createSettingsSnapshot,
  isSettingsDirty,
  validateSlaDefaults,
  type AutoApproverDefaults,
  type OrganizationProfile,
  type SlaDefaults,
  type WorkflowDefaults,
} from "~/composables/adminSettingsForm";
import { describe, expect, it } from "vitest";

describe("admin settings form interaction logic", () => {
  const workflowDefaults: WorkflowDefaults = {
    approvalTypeDefault: "LINEAR",
    requireDefaultApprovers: true,
  };

  const slaDefaults: SlaDefaults = {
    lowHours: 72,
    mediumHours: 48,
    highHours: 24,
    criticalHours: 8,
    warningBeforeHours: 1,
  };

  const autoApproverDefaults: AutoApproverDefaults = {
    userIds: ["11111111-1111-1111-1111-111111111111"],
    groupIds: ["22222222-2222-2222-2222-222222222222"],
  };

  const profile: OrganizationProfile = {
    name: "Ronin Limited",
    primaryContactEmail: "admin@ronin.test",
    timezone: "UTC",
  };

  it("marks form as clean when snapshot matches current values", () => {
    const snapshot = createSettingsSnapshot(
      profile,
      workflowDefaults,
      slaDefaults,
      autoApproverDefaults,
    );

    expect(
      isSettingsDirty(
        snapshot,
        profile,
        workflowDefaults,
        slaDefaults,
        autoApproverDefaults,
      ),
    ).toBe(false);
  });

  it("marks form as dirty when workflow value changes", () => {
    const snapshot = createSettingsSnapshot(
      profile,
      workflowDefaults,
      slaDefaults,
      autoApproverDefaults,
    );

    expect(
      isSettingsDirty(
        snapshot,
        profile,
        { ...workflowDefaults, approvalTypeDefault: "NON_LINEAR" },
        slaDefaults,
        autoApproverDefaults,
      ),
    ).toBe(true);
  });

  it("marks form as dirty when auto-approver defaults change", () => {
    const snapshot = createSettingsSnapshot(
      profile,
      workflowDefaults,
      slaDefaults,
      autoApproverDefaults,
    );

    expect(
      isSettingsDirty(snapshot, profile, workflowDefaults, slaDefaults, {
        ...autoApproverDefaults,
        userIds: [
          ...autoApproverDefaults.userIds,
          "33333333-3333-3333-3333-333333333333",
        ],
      }),
    ).toBe(true);
  });

  it("rejects invalid warning-before-hours that are not less than every deadline", () => {
    const invalidSlaDefaults: SlaDefaults = {
      ...slaDefaults,
      warningBeforeHours: 8,
    };

    expect(validateSlaDefaults(invalidSlaDefaults)).toBe(
      "Warning hours must be less than all SLA deadline values.",
    );
  });

  it("accepts valid warning-before-hours", () => {
    expect(validateSlaDefaults(slaDefaults)).toBeNull();
  });

  it("builds stable patch payload from current settings", () => {
    expect(
      buildSettingsPatchPayload(
        profile,
        workflowDefaults,
        slaDefaults,
        autoApproverDefaults,
      ),
    ).toEqual({
      profile,
      workflowDefaults,
      slaDefaults,
      autoApproverDefaults,
    });
  });
});
