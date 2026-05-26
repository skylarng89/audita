import { describe, expect, it, vi } from "vitest";
import {
  runInviteAcceptanceSubmit,
  type InviteAcceptanceFormState,
} from "~/composables/inviteAcceptance";

function createState(): InviteAcceptanceFormState {
  return {
    password: "StrongPass1!A",
    confirmPassword: "StrongPass1!A",
    done: false,
    error: "",
    isLoading: false,
  };
}

function deferred<T>() {
  let resolvePromise: (value: T) => void = () => {};
  const promise = new Promise<T>((resolve) => {
    resolvePromise = resolve;
  });
  return { promise, resolve: resolvePromise };
}

describe("invite acceptance form submit", () => {
  it("toggles loading during async submit and sets success state", async () => {
    const state = createState();
    const pending = deferred<{ done: boolean; error: string }>();
    const submitInviteAcceptance = vi.fn().mockReturnValue(pending.promise);

    const submitPromise = runInviteAcceptanceSubmit({
      state,
      routeToken: "invite-token",
      routeTenant: "cm",
      authTenantSlug: "cm",
      submitInviteAcceptanceHandler: submitInviteAcceptance,
      dependencies: {
        acceptInvite: vi.fn(),
        fetchStatus: async () => ({
          onboardingCompleted: true,
          tenantSlug: "pixelpay-systems-limited",
        }),
        setTenantSlug: vi.fn(),
        resolveApiErrorMessage: () => "Invite expired.",
      },
    });

    expect(state.isLoading).toBe(true);

    pending.resolve({ done: true, error: "" });
    await submitPromise;

    expect(state.isLoading).toBe(false);
    expect(state.done).toBe(true);
    expect(state.error).toBe("");
  });

  it("writes validation errors into state", async () => {
    const state = createState();
    state.confirmPassword = "DifferentPass1!A";
    const submitInviteAcceptance = vi.fn().mockResolvedValue({
      done: false,
      error: "Passwords do not match.",
    });

    await runInviteAcceptanceSubmit({
      state,
      routeToken: "invite-token",
      routeTenant: "cm",
      authTenantSlug: "cm",
      submitInviteAcceptanceHandler: submitInviteAcceptance,
      dependencies: {
        acceptInvite: vi.fn(),
        fetchStatus: async () => null,
        setTenantSlug: vi.fn(),
        resolveApiErrorMessage: () => "Invite expired.",
      },
    });

    expect(state.done).toBe(false);
    expect(state.error).toBe("Passwords do not match.");
    expect(state.isLoading).toBe(false);
  });
});
