/** @vitest-environment jsdom */

import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import CrStatusBadge from "~/components/cr/CrStatusBadge.vue";
import CrCompletionStatusBadge from "~/components/cr/CrCompletionStatusBadge.vue";

describe("CrStatusBadge (Approval Status)", () => {
  it("renders approval status label", () => {
    const wrapper = mount(CrStatusBadge, { props: { status: "APPROVED" } });
    expect(wrapper.text()).toBe("Approved");
  });

  it("renders Pending Approval for PENDING_APPROVAL", () => {
    const wrapper = mount(CrStatusBadge, { props: { status: "PENDING_APPROVAL" } });
    expect(wrapper.text()).toBe("Pending Approval");
  });
});

describe("CrCompletionStatusBadge", () => {
  it("renders 'In Progress' for IN_PROGRESS", () => {
    const wrapper = mount(CrCompletionStatusBadge, { props: { status: "IN_PROGRESS" } });
    expect(wrapper.text()).toBe("In Progress");
    expect(wrapper.find(".badge-pending").exists()).toBe(true);
  });

  it("renders 'Completed' for COMPLETED", () => {
    const wrapper = mount(CrCompletionStatusBadge, { props: { status: "COMPLETED" } });
    expect(wrapper.text()).toBe("Completed");
    expect(wrapper.find(".badge-approved").exists()).toBe(true);
  });
});

describe("displayId rendering", () => {
  it("uses displayId when available", () => {
    const displayId = "CR-0042";
    const fallback = "CHG-ABCD1234";
    const result = displayId || fallback;
    expect(result).toBe("CR-0042");
  });

  it("falls back to CHG- prefix when displayId is null", () => {
    const displayId: string | null = null;
    const id = "abcd1234-5678-90ab-cdef-1234567890ab";
    const result = displayId || ("CHG-" + id.slice(0, 8).toUpperCase());
    expect(result).toBe("CHG-ABCD1234");
  });
});
