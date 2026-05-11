import {
  buildSettingsPatchPayload,
  createSettingsSnapshot,
  isSettingsDirty,
  validateSlaDefaults,
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

  it("marks form as clean when snapshot matches current values", () => {
    const snapshot = createSettingsSnapshot(workflowDefaults, slaDefaults);

    expect(isSettingsDirty(snapshot, workflowDefaults, slaDefaults)).toBe(
      false,
    );
  });

  it("marks form as dirty when workflow value changes", () => {
    const snapshot = createSettingsSnapshot(workflowDefaults, slaDefaults);

    expect(
      isSettingsDirty(
        snapshot,
        { ...workflowDefaults, approvalTypeDefault: "NON_LINEAR" },
        slaDefaults,
      ),
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
    expect(buildSettingsPatchPayload(workflowDefaults, slaDefaults)).toEqual({
      workflowDefaults,
      slaDefaults,
    });
  });
});
