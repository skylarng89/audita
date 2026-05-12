import { resolveTenantSlug } from "~/composables/tenantResolution";
import { describe, expect, it } from "vitest";

describe("tenant resolution", () => {
  it("resolves tenant slug from the first subdomain", () => {
    expect(
      resolveTenantSlug({
        hostname: "acme.audita.test",
        queryTenant: null,
        isDev: false,
      }),
    ).toBe("acme");
  });

  it("falls back to the dev tenant query only outside real subdomains", () => {
    expect(
      resolveTenantSlug({
        hostname: "localhost:3000",
        queryTenant: "acme",
        isDev: true,
      }),
    ).toBe("acme");
  });

  it("ignores dev tenant query in non-dev builds", () => {
    expect(
      resolveTenantSlug({
        hostname: "localhost:3000",
        queryTenant: "acme",
        isDev: false,
      }),
    ).toBeNull();
  });
});
