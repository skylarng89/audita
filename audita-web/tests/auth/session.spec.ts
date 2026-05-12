import {
  computeTokenExpiresAt,
  hasActiveAccessToken,
  shouldAttemptTokenRefresh,
} from "~/composables/authSession";
import { describe, expect, it } from "vitest";

describe("auth session helpers", () => {
  it("computes token expiry from expiresIn seconds", () => {
    expect(computeTokenExpiresAt(900, 1_000)).toBe(901_000);
  });

  it("treats access tokens without an expiry as inactive", () => {
    expect(hasActiveAccessToken("access-token", null, 1_000)).toBe(false);
  });

  it("treats unexpired access tokens as active", () => {
    expect(hasActiveAccessToken("access-token", 2_000, 1_000)).toBe(true);
  });

  it("refreshes only once on 401 responses", () => {
    expect(
      shouldAttemptTokenRefresh({
        alreadyRetried: false,
        isAuthenticated: true,
        isRefreshEndpoint: false,
        responseStatus: 401,
      }),
    ).toBe(true);

    expect(
      shouldAttemptTokenRefresh({
        alreadyRetried: false,
        isAuthenticated: true,
        isRefreshEndpoint: false,
        responseStatus: 403,
      }),
    ).toBe(false);
  });
});
