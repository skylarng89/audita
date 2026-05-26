import { describe, expect, it } from "vitest";
import {
  normalizeInviteQueryValue,
  resolveInviteTenantSlug,
  resolveInviteToken,
} from "~/composables/inviteTenantResolution";

describe("invite tenant resolution", () => {
  it("prefers canonical tenant slug over alias values", () => {
    const tenantSlug = resolveInviteTenantSlug({
      statusTenantSlug: "pixelpay-systems-limited",
      authTenantSlug: "cm",
      queryTenant: "cm",
    });

    expect(tenantSlug).toBe("pixelpay-systems-limited");
  });

  it("falls back to auth tenant slug when status has no tenant", () => {
    const tenantSlug = resolveInviteTenantSlug({
      statusTenantSlug: null,
      authTenantSlug: "pixelpay-systems-limited",
      queryTenant: "cm",
    });

    expect(tenantSlug).toBe("pixelpay-systems-limited");
  });

  it("uses query tenant as last fallback after normalization", () => {
    const tenantSlug = resolveInviteTenantSlug({
      statusTenantSlug: null,
      authTenantSlug: null,
      queryTenant: "  pixelpay-systems-limited  ",
    });

    expect(tenantSlug).toBe("pixelpay-systems-limited");
  });

  it("normalizes token query values", () => {
    expect(resolveInviteToken("  abc123  ")).toBe("abc123");
    expect(resolveInviteToken("   ")).toBeNull();
    expect(resolveInviteToken(undefined)).toBeNull();
  });

  it("normalizes only non-empty string values", () => {
    expect(normalizeInviteQueryValue(" cm ")).toBe("cm");
    expect(normalizeInviteQueryValue(42)).toBeNull();
    expect(normalizeInviteQueryValue(null)).toBeNull();
  });
});
