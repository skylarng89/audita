import { beforeEach, describe, expect, it, vi } from "vitest";

const mockUseAuthStore = vi.fn();
const mockNavigateTo = vi.fn((path: string) => path);

vi.mock("~/stores/auth", () => ({
  useAuthStore: mockUseAuthStore,
}));

describe("auth.global middleware", () => {
  beforeEach(() => {
    vi.resetModules();
    mockUseAuthStore.mockReset();
    mockNavigateTo.mockClear();

    vi.stubGlobal("defineNuxtRouteMiddleware", (fn: unknown) => fn);
    vi.stubGlobal("navigateTo", mockNavigateTo);
  });

  it("redirects unauthenticated users from /platform to sign-in with redirect param", async () => {
    mockUseAuthStore.mockReturnValue({ isAuthenticated: false });

    const { default: middleware } = await import("~/middleware/auth.global");
    type To = Parameters<typeof middleware>[0];
    type From = Parameters<typeof middleware>[1];
    await middleware(
      { path: "/platform", fullPath: "/platform" } as To,
      { path: "/" } as From,
    );

    expect(mockNavigateTo).toHaveBeenCalledWith(
      "/auth/sign-in?redirect=%2Fplatform",
    );
  });

  it("allows unauthenticated users to access /platform/bootstrap", async () => {
    mockUseAuthStore.mockReturnValue({ isAuthenticated: false });

    const { default: middleware } = await import("~/middleware/auth.global");
    type To = Parameters<typeof middleware>[0];
    type From = Parameters<typeof middleware>[1];
    const result = await middleware(
      { path: "/platform/bootstrap" } as To,
      { path: "/" } as From,
    );

    expect(result).toBeUndefined();
    expect(mockNavigateTo).not.toHaveBeenCalled();
  });

  it("allows authenticated users to access protected routes", async () => {
    mockUseAuthStore.mockReturnValue({ isAuthenticated: true });

    const { default: middleware } = await import("~/middleware/auth.global");
    type To = Parameters<typeof middleware>[0];
    type From = Parameters<typeof middleware>[1];
    const result = await middleware(
      { path: "/platform" } as To,
      { path: "/" } as From,
    );

    expect(result).toBeUndefined();
    expect(mockNavigateTo).not.toHaveBeenCalled();
  });
});
