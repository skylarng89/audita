import { beforeEach, describe, expect, it, vi } from "vitest";

const mockUseAuthStore = vi.fn();
const mockUseNuxtApp = vi.fn();

vi.mock("~/stores/auth", () => ({
  useAuthStore: mockUseAuthStore,
}));

describe("tenant middleware", () => {
  beforeEach(() => {
    vi.resetModules();
    mockUseAuthStore.mockReset();
    mockUseNuxtApp.mockReset();

    vi.stubGlobal("defineNuxtRouteMiddleware", (fn: unknown) => fn);
    vi.stubGlobal("useNuxtApp", mockUseNuxtApp);
    vi.stubGlobal("window", {
      location: { hostname: "beta.audita.test" },
    });
  });

  it("logs out authenticated users when the resolved tenant changes", async () => {
    const logout = vi.fn().mockResolvedValue(undefined);
    mockUseAuthStore.mockReturnValue({
      isAuthenticated: true,
      logout,
      tenantSlug: "acme",
    });
    mockUseNuxtApp.mockReturnValue({ ssrContext: null });

    const { default: middleware } = await import("~/middleware/tenant");
    type To = Parameters<typeof middleware>[0];
    type From = Parameters<typeof middleware>[1];
    const result = await middleware(
      { path: "/dashboard", query: {} } as To,
      { path: "/" } as From,
    );

    expect(result).toBeUndefined();
    expect(logout).toHaveBeenCalledOnce();
  });
});
