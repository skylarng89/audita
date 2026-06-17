/** @vitest-environment jsdom */

import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import type {
  ChangeRequest,
  CustomFieldDefinition,
  ChangeRequestCustomFieldValue,
} from "~/types";

vi.mock("~/composables/useApi", () => ({
  useApi: () => vi.fn(),
}));

vi.mock("~/composables/timezone", () => ({
  formatDateTimeInTenantTimezone: (v: string | null) => v ?? "—",
}));

vi.mock("~/composables/richText", () => ({
  normalizeRichTextHtml: (v: string | null | undefined) => v ?? "",
  buildRichTextExtensions: () => [],
}));

import CrRequestOverviewPanel from "~/components/cr/CrRequestOverviewPanel.vue";
import CrCompletionStatusControl from "~/components/cr/CrCompletionStatusControl.vue";
import CrCompletionStatusBadge from "~/components/cr/CrCompletionStatusBadge.vue";

const completionControlGlobal = {
  components: { CrCompletionStatusBadge },
};

function buildCr(overrides: Partial<ChangeRequest> = {}): ChangeRequest {
  return {
    id: "cr-1",
    displayId: "CR-0001",
    title: "Test",
    description: "<p>Hello</p>",
    priority: "MEDIUM",
    riskLevel: "HIGH",
    category: "Infrastructure",
    status: "APPROVED",
    approvalType: "LINEAR",
    approvalStatus: "APPROVED",
    completionStatus: "IN_PROGRESS",
    approvalLocked: false,
    workflowMode: "DELIVERY_PIPELINE",
    requestDepartmentId: null,
    destinationDepartmentId: null,
    scheduledStart: "2026-06-01T10:00:00Z",
    scheduledEnd: "2026-06-05T18:00:00Z",
    affectedSystems: ["API Gateway", "Auth Service"],
    slaDeadline: "2026-06-10T00:00:00Z",
    slaBreached: false,
    createdBy: "user-1",
    createdByEmail: "test@example.com",
    createdByFullName: "Test User",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

describe("CrRequestOverviewPanel", () => {
  it("renders description", () => {
    const wrapper = mount(CrRequestOverviewPanel, {
      props: {
        changeRequest: buildCr(),
        customFields: [],
        fieldDefinitions: [],
        localFieldValues: {},
        linkedRequestIds: [],
      },
    });
    expect(wrapper.text()).toContain("Description");
    expect(wrapper.html()).toContain("Hello");
  });

  it("renders priority, risk level, and category", () => {
    const wrapper = mount(CrRequestOverviewPanel, {
      props: {
        changeRequest: buildCr(),
        customFields: [],
        fieldDefinitions: [],
        localFieldValues: {},
        linkedRequestIds: [],
      },
    });
    expect(wrapper.text()).toContain("MEDIUM");
    expect(wrapper.text()).toContain("HIGH");
    expect(wrapper.text()).toContain("Infrastructure");
  });

  it("renders affected systems", () => {
    const wrapper = mount(CrRequestOverviewPanel, {
      props: {
        changeRequest: buildCr(),
        customFields: [],
        fieldDefinitions: [],
        localFieldValues: {},
        linkedRequestIds: [],
      },
    });
    expect(wrapper.text()).toContain("API Gateway");
    expect(wrapper.text()).toContain("Auth Service");
  });

  it("renders custom fields when present", () => {
    const defs: CustomFieldDefinition[] = [
      {
        id: "cf-1",
        label: "Environment",
        fieldType: "TEXT",
        isRequired: false,
        options: [],
        displayOrder: 1,
      },
    ];
    const values: Record<string, string> = { "cf-1": "Production" };
    const wrapper = mount(CrRequestOverviewPanel, {
      props: {
        changeRequest: buildCr(),
        customFields: [],
        fieldDefinitions: defs,
        localFieldValues: values,
        linkedRequestIds: [],
      },
    });
    expect(wrapper.text()).toContain("Environment");
    expect(wrapper.text()).toContain("Production");
  });

  it("renders linked requests when present", () => {
    const wrapper = mount(CrRequestOverviewPanel, {
      props: {
        changeRequest: buildCr(),
        customFields: [],
        fieldDefinitions: [],
        localFieldValues: {},
        linkedRequestIds: ["cr-2", "cr-3"],
      },
    });
    expect(wrapper.text()).toContain("Linked Requests");
    expect(wrapper.text()).toContain("cr-2");
    expect(wrapper.text()).toContain("cr-3");
  });

  it("shows 'No description' when description is null", () => {
    const wrapper = mount(CrRequestOverviewPanel, {
      props: {
        changeRequest: buildCr({ description: null }),
        customFields: [],
        fieldDefinitions: [],
        localFieldValues: {},
        linkedRequestIds: [],
      },
    });
    expect(wrapper.text()).toContain("No description");
  });
});

describe("CrCompletionStatusControl", () => {
  it("renders completion status badge", () => {
    const wrapper = mount(CrCompletionStatusControl, {
      props: {
        requestId: "cr-1",
        completionStatus: "IN_PROGRESS",
        approvalStatus: "APPROVED",
        workflowMode: "DELIVERY_PIPELINE",
        deploymentDone: false,
      },
      global: completionControlGlobal,
    });
    expect(wrapper.text()).toContain("In Progress");
  });

  it("shows 'Mark Complete' button when conditions are met", () => {
    const wrapper = mount(CrCompletionStatusControl, {
      props: {
        requestId: "cr-1",
        completionStatus: "IN_PROGRESS",
        approvalStatus: "APPROVED",
        workflowMode: "DELIVERY_PIPELINE",
        deploymentDone: true,
      },
      global: completionControlGlobal,
    });
    expect(wrapper.find("button").exists()).toBe(true);
    expect(wrapper.text()).toContain("Mark Complete");
  });

  it("hides 'Mark Complete' when already completed", () => {
    const wrapper = mount(CrCompletionStatusControl, {
      props: {
        requestId: "cr-1",
        completionStatus: "COMPLETED",
        approvalStatus: "APPROVED",
        workflowMode: "DELIVERY_PIPELINE",
        deploymentDone: true,
      },
      global: completionControlGlobal,
    });
    expect(wrapper.text()).toContain("Completed");
    expect(wrapper.find("button").exists()).toBe(false);
  });

  it("hides 'Mark Complete' when deployment not done", () => {
    const wrapper = mount(CrCompletionStatusControl, {
      props: {
        requestId: "cr-1",
        completionStatus: "IN_PROGRESS",
        approvalStatus: "APPROVED",
        workflowMode: "DELIVERY_PIPELINE",
        deploymentDone: false,
      },
      global: completionControlGlobal,
    });
    expect(wrapper.find("button").exists()).toBe(false);
  });

  it("shows 'Mark Complete' when approval-only and approved", () => {
    const wrapper = mount(CrCompletionStatusControl, {
      props: {
        requestId: "cr-1",
        completionStatus: "IN_PROGRESS",
        approvalStatus: "APPROVED",
        workflowMode: "APPROVAL_ONLY",
        deploymentDone: false,
      },
      global: completionControlGlobal,
    });
    expect(wrapper.find("button").exists()).toBe(true);
  });

  it("emits 'completed' when Mark Complete is clicked", async () => {
    const wrapper = mount(CrCompletionStatusControl, {
      props: {
        requestId: "cr-1",
        completionStatus: "IN_PROGRESS",
        approvalStatus: "APPROVED",
        workflowMode: "DELIVERY_PIPELINE",
        deploymentDone: true,
      },
      global: completionControlGlobal,
    });
    await wrapper.find("button").trigger("click");
    expect(wrapper.emitted("completed")).toBeTruthy();
  });
});
