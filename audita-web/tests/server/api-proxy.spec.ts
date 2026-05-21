import { describe, expect, it } from "vitest";
import {
  buildProxyTarget,
  sanitizeProxyHeaders,
  validateProxyRequest,
} from "~/server/utils/apiProxy";

describe("api proxy hardening", () => {
  it("allows only safe proxy headers", () => {
    const sanitized = sanitizeProxyHeaders({
      authorization: "Bearer token",
      "content-length": "123",
      cookie: "refreshToken=abc",
      "x-tenant-slug": "tenant-acme",
      "x-idempotency-key": "req-12345678",
      origin: "https://evil.example",
      referer: "https://evil.example/app",
      host: "evil.example",
      "x-forwarded-host": "evil.example",
    });

    expect(sanitized.authorization).toBe("Bearer token");
    expect(sanitized["content-length"]).toBe("123");
    expect(sanitized.cookie).toBe("refreshToken=abc");
    expect(sanitized["x-tenant-slug"]).toBe("tenant-acme");
    expect(sanitized["x-idempotency-key"]).toBe("req-12345678");
    expect(sanitized.origin).toBeUndefined();
    expect(sanitized.referer).toBeUndefined();
    expect(sanitized.host).toBeUndefined();
    expect(sanitized["x-forwarded-host"]).toBeUndefined();
  });

  it("rejects unsupported methods", () => {
    expect(() => validateProxyRequest("TRACE", "/api/v1/auth/login", null)).toThrow(
      "Unsupported proxy method",
    );
  });

  it("rejects unsafe traversal paths", () => {
    expect(() => validateProxyRequest("GET", "/api/../secrets", null)).toThrow(
      "Unsafe proxy path",
    );
  });

  it("rejects unsupported body content-type for mutating requests", () => {
    expect(() =>
      validateProxyRequest("POST", "/api/v1/auth/login", "application/xml"),
    ).toThrow("Unsupported content type");
  });

  it("builds deterministic target url", () => {
    const target = buildProxyTarget(
      "http://audita-api:8080/",
      "/api/v1/change-requests",
      "?page=1&size=50",
    );
    expect(target).toBe("http://audita-api:8080/api/v1/change-requests?page=1&size=50");
  });
});
