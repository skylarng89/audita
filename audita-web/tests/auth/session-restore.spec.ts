import { describe, expect, it, vi } from "vitest";
import {
  ApiContractMismatchError,
  restoreSessionFromCookie,
} from "~/composables/sessionRestore";

interface MockAuthStore {
  tenantSlug: string | null;
  setAuth: ReturnType<typeof vi.fn>;
}

describe("session restore", () => {
  it("sends X-Tenant-Slug during session restore", async () => {
    const setAuth = vi.fn();
    const auth = {
      tenantSlug: "ronin-limited",
      setAuth,
    } as unknown as MockAuthStore;

    const raw = vi.fn().mockResolvedValue({
      _data: {
        accessToken: "token",
        tokenType: "Bearer",
        expiresIn: 3600,
        userId: "u1",
        email: "dev@example.com",
        fullName: "Dev User",
        role: "Requester",
        tenantSlug: "ronin-limited",
      },
      headers: new Headers({ "X-Audita-Api-Contract": "v1" }),
    });

    vi.stubGlobal("$fetch", { raw });

    await restoreSessionFromCookie(auth as never, "v1", false);

    const call = raw.mock.calls[0];
    expect(call[0]).toBe("/api/v1/auth/session");
    const headers = call[1].headers as Headers;
    expect(headers.get("X-Tenant-Slug")).toBe("ronin-limited");
    expect(setAuth).toHaveBeenCalledTimes(1);
  });

  it("throws on API contract mismatch", async () => {
    const auth = {
      tenantSlug: null,
      setAuth: vi.fn(),
    } as unknown as MockAuthStore;

    const raw = vi.fn().mockResolvedValue({
      _data: {
        accessToken: "token",
        tokenType: "Bearer",
        expiresIn: 3600,
        userId: "u1",
        email: "dev@example.com",
        fullName: "Dev User",
        role: "Requester",
        tenantSlug: null,
      },
      headers: new Headers({ "X-Audita-Api-Contract": "v2" }),
    });

    vi.stubGlobal("$fetch", { raw });

    await expect(
      restoreSessionFromCookie(auth as never, "v1", false),
    ).rejects.toBeInstanceOf(ApiContractMismatchError);
  });
});
