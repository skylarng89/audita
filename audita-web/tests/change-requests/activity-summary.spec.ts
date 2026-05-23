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
});
