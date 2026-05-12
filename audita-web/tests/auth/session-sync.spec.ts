import {
  createAuthSessionSyncEvent,
  type AuthSessionSyncEvent,
} from "~/composables/authSessionSync";
import { describe, expect, it, vi } from "vitest";

describe("auth session sync helpers", () => {
  it("creates a token-free cross-tab session event", () => {
    vi.spyOn(Date, "now").mockReturnValue(123_456);

    const event: AuthSessionSyncEvent = createAuthSessionSyncEvent(
      "session-restored",
      "tab-1",
    );

    expect(event).toEqual({
      sourceTabId: "tab-1",
      timestamp: 123_456,
      type: "session-restored",
    });
  });
});
