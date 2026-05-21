import {
  formatDateInTimezone,
  formatDateTimeInTimezone,
  getIanaTimezones,
  normalizeTimezone,
} from "~/composables/timezone";
import { describe, expect, it } from "vitest";

describe("timezone helpers", () => {
  it("returns an IANA timezone list containing UTC", () => {
    const zones = getIanaTimezones();
    expect(zones.length).toBeGreaterThan(0);
    expect(zones).toContain("UTC");
  });

  it("normalizes unknown values to UTC", () => {
    expect(normalizeTimezone("Not/AZone")).toBe("UTC");
    expect(normalizeTimezone("   ")).toBe("UTC");
  });

  it("formats ISO date in provided timezone", () => {
    const formatted = formatDateInTimezone("2026-05-21T12:00:00Z", "UTC");
    expect(formatted).toContain("2026");
  });

  it("formats ISO datetime in provided timezone", () => {
    const formatted = formatDateTimeInTimezone("2026-05-21T12:00:00Z", "UTC");
    expect(formatted).toContain("2026");
  });
});
