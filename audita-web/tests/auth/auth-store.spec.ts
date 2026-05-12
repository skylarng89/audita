import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "~/stores/auth";

function createLocalStorageMock() {
  const storage = new Map<string, string>();
  return {
    clear: () => storage.clear(),
    getItem: (key: string) => storage.get(key) ?? null,
    removeItem: (key: string) => storage.delete(key),
    setItem: (key: string, value: string) => storage.set(key, value),
  };
}

describe("auth store tenant slug persistence", () => {
  beforeEach(() => {
    setActivePinia(createPinia());

    vi.stubGlobal("useOnboarding", () => ({
      invalidateStatus: vi.fn(),
    }));

    vi.stubGlobal("localStorage", createLocalStorageMock());
  });

  it("persists tenant slug when set", () => {
    const auth = useAuthStore();

    auth.setTenantSlug("ronin-limited");

    expect(localStorage.getItem("audita-tenant-slug")).toBe("ronin-limited");
  });

  it("hydrates tenant slug from persisted storage", () => {
    const auth = useAuthStore();
    localStorage.setItem("audita-tenant-slug", "ronin-limited");

    auth.hydrateTenantSlug();

    expect(auth.tenantSlug).toBe("ronin-limited");
  });

  it("clears tenant slug persistence on clearAuth", () => {
    const auth = useAuthStore();
    auth.setTenantSlug("ronin-limited");

    auth.clearAuth();

    expect(localStorage.getItem("audita-tenant-slug")).toBeNull();
  });
});
