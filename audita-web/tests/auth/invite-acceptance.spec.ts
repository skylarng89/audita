import { describe, expect, it, vi } from "vitest";
import { submitInviteAcceptance } from "~/composables/inviteAcceptance";

describe("invite acceptance submit flow", () => {
  it("uses canonical tenant slug from status when query tenant is alias", async () => {
    const acceptInvite = vi.fn().mockResolvedValue(undefined);
    const setTenantSlug = vi.fn();
    const fetchStatus = vi.fn().mockResolvedValue({
      onboardingCompleted: true,
      tenantSlug: "pixelpay-systems-limited",
    });

    const result = await submitInviteAcceptance({
      password: "StrongPass1!A",
      confirmPassword: "StrongPass1!A",
      routeToken: "invite-token",
      routeTenant: "cm",
      authTenantSlug: "cm",
      acceptInvite,
      fetchStatus,
      setTenantSlug,
      resolveApiErrorMessage: () => "This invite link is invalid or has expired.",
    });

    expect(result).toEqual({ done: true, error: "" });
    expect(acceptInvite).toHaveBeenCalledWith(
      "invite-token",
      "StrongPass1!A",
      "pixelpay-systems-limited",
    );
    expect(setTenantSlug).toHaveBeenCalledWith("pixelpay-systems-limited");
  });

  it("returns invalid invite link when token or tenant is unavailable", async () => {
    const acceptInvite = vi.fn();

    const result = await submitInviteAcceptance({
      password: "StrongPass1!A",
      confirmPassword: "StrongPass1!A",
      routeToken: "",
      routeTenant: "",
      authTenantSlug: null,
      acceptInvite,
      fetchStatus: async () => null,
      setTenantSlug: vi.fn(),
      resolveApiErrorMessage: () => "This invite link is invalid or has expired.",
    });

    expect(result).toEqual({ done: false, error: "Invalid invite link." });
    expect(acceptInvite).not.toHaveBeenCalled();
  });

  it("maps API failures through resolver", async () => {
    const acceptInvite = vi.fn().mockRejectedValue(new Error("boom"));

    const result = await submitInviteAcceptance({
      password: "StrongPass1!A",
      confirmPassword: "StrongPass1!A",
      routeToken: "invite-token",
      routeTenant: "pixelpay-systems-limited",
      authTenantSlug: null,
      acceptInvite,
      fetchStatus: async () => null,
      setTenantSlug: vi.fn(),
      resolveApiErrorMessage: () => "Invite expired.",
    });

    expect(result).toEqual({ done: false, error: "Invite expired." });
  });
});
