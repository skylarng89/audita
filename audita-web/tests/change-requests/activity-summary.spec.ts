import { describe, expect, it } from "vitest";
import { buildActivitySummary } from "~/composables/activitySummary";

describe("buildActivitySummary", () => {
  it("prefers explicit reason payload", () => {
    const result = buildActivitySummary({
      actionType: "CR_APPROVER_ADDED",
      payload: { reason: "Manual override" },
    } as any);

    expect(result).toBe("Manual override");
  });

  it("formats approver-added actions", () => {
    const result = buildActivitySummary({
      actionType: "CR_APPROVER_ADDED",
      payload: {},
    } as any);

    expect(result).toBe("Added approver.");
  });

  it("formats group add with count", () => {
    const result = buildActivitySummary({
      actionType: "CR_APPROVER_GROUP_ADDED",
      payload: { count: 2 },
    } as any);

    expect(result).toBe("Added 2 approvers from group.");
  });

  it("formats requirement change to optional", () => {
    const result = buildActivitySummary({
      actionType: "CR_APPROVER_REQUIREMENT_CHANGED",
      payload: { isRequired: false },
    } as any);

    expect(result).toBe("Marked approver as optional.");
  });

  it("formats reorder count", () => {
    const result = buildActivitySummary({
      actionType: "CR_APPROVERS_REORDERED",
      payload: { count: 1 },
    } as any);

    expect(result).toBe("Reordered 1 approver.");
  });

  it("returns null for unsupported action with no known payload", () => {
    const result = buildActivitySummary({
      actionType: "CR_UPDATED",
      payload: {},
    } as any);

    expect(result).toBeNull();
  });

  it("formats UAT_CREATED with reason", () => {
    const result = buildActivitySummary({
      actionType: "UAT_CREATED",
      payload: { reason: "UAT plan initiated" },
    } as any);

    expect(result).toBe("UAT plan initiated");
  });

  it("formats UAT_PROMOTED with reason", () => {
    const result = buildActivitySummary({
      actionType: "UAT_PROMOTED",
      payload: { reason: "UAT passed, promoting to deployment" },
    } as any);

    expect(result).toBe("UAT passed, promoting to deployment");
  });

  it("formats DEPLOYMENT_CREATED with reason", () => {
    const result = buildActivitySummary({
      actionType: "DEPLOYMENT_CREATED",
      payload: { reason: "Deployment initiated after UAT promotion" },
    } as any);

    expect(result).toBe("Deployment initiated after UAT promotion");
  });

  it("formats CR_COMPLETED with reason", () => {
    const result = buildActivitySummary({
      actionType: "CR_COMPLETED",
      payload: { reason: "All steps verified" },
    } as any);

    expect(result).toBe("All steps verified");
  });

  it("returns null for UAT_CREATED without reason or known payload shape", () => {
    const result = buildActivitySummary({
      actionType: "UAT_CREATED",
      payload: {},
    } as any);

    expect(result).toBeNull();
  });

  it("returns null for DEPLOYMENT_CREATED without reason", () => {
    const result = buildActivitySummary({
      actionType: "DEPLOYMENT_CREATED",
      payload: {},
    } as any);

    expect(result).toBeNull();
  });

  it("formats CR_LINKED with reason", () => {
    const result = buildActivitySummary({
      actionType: "CR_LINKED",
      payload: { reason: "Linked to related request" },
    } as any);

    expect(result).toBe("Linked to related request");
  });
});
