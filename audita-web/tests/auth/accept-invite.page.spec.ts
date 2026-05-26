/** @vitest-environment jsdom */

import { mount } from "@vue/test-utils";
import { computed, reactive, ref } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AcceptInvitePage from "~/pages/auth/accept-invite.vue";

describe("accept invite page", () => {
  const runInviteAcceptanceSubmit = vi.fn();

  beforeEach(() => {
    runInviteAcceptanceSubmit.mockReset();

    vi.stubGlobal("definePageMeta", vi.fn());
    vi.stubGlobal("useHead", vi.fn());
    vi.stubGlobal("reactive", reactive);
    vi.stubGlobal("ref", ref);
    vi.stubGlobal("computed", computed);
    vi.stubGlobal("useRoute", () => ({
      query: { token: "invite-token", tenant: "cm" },
    }));
    vi.stubGlobal("useAuthStore", () => ({
      tenantSlug: "cm",
      setTenantSlug: vi.fn(),
    }));
    vi.stubGlobal("useOnboarding", () => ({
      fetchStatus: vi.fn(),
    }));
    vi.stubGlobal("useAuth", () => ({
      acceptInvite: vi.fn(),
    }));
    vi.stubGlobal("resolveApiErrorMessage", vi.fn());
    vi.stubGlobal("runInviteAcceptanceSubmit", runInviteAcceptanceSubmit);
  });

  it("submits form and renders success state", async () => {
    runInviteAcceptanceSubmit.mockImplementationOnce(async ({ state }) => {
      state.done = true;
      state.error = "";
      state.isLoading = false;
    });

    const wrapper = mount(AcceptInvitePage, {
      global: {
        stubs: {
          NuxtLink: {
            template: "<a><slot /></a>",
          },
          SharedAppButton: {
            props: ["type", "size", "loading"],
            template: '<button :type="type"><slot /></button>',
          },
        },
      },
    });

    await wrapper.get("#invite-new-password").setValue("StrongPass1!A");
    await wrapper.get("#invite-confirm-password").setValue("StrongPass1!A");
    await wrapper.get("form").trigger("submit.prevent");

    expect(runInviteAcceptanceSubmit).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain("Account activated!");
  });
});
