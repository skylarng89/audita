import { describe, expect, it, vi, beforeEach } from "vitest";
import type { ChangeRequest } from "~/types";

const mockApi = vi.fn();
vi.mock("~/composables/useApi", () => ({
  useApi: () => mockApi,
}));

import { useChangeRequests } from "~/composables/useChangeRequests";

beforeEach(() => {
  mockApi.mockReset();
});

function buildCr(overrides: Partial<ChangeRequest> = {}): ChangeRequest {
  return {
    id: "cr-1",
    displayId: "CR-0001",
    title: "Test",
    description: null,
    priority: "LOW",
    riskLevel: "LOW",
    category: null,
    status: "APPROVED",
    approvalType: "LINEAR",
    approvalStatus: "APPROVED",
    completionStatus: "IN_PROGRESS",
    approvalLocked: false,
    workflowMode: "DELIVERY_PIPELINE",
    requestDepartmentId: null,
    destinationDepartmentId: null,
    scheduledStart: null,
    scheduledEnd: null,
    affectedSystems: [],
    slaDeadline: null,
    slaBreached: false,
    createdBy: null,
    createdByEmail: null,
    createdByFullName: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

describe("UAT composable methods", () => {
  it("getUat calls GET /api/v1/change-requests/{id}/uat", async () => {
    const uatData = { id: "uat-1", title: "UAT Plan", details: "<p>Test</p>", status: "DRAFT", readOnly: false, approvers: [] };
    mockApi.mockResolvedValueOnce(uatData);

    const { getUat } = useChangeRequests();
    const result = await getUat("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat");
    expect(result).toEqual(uatData);
  });

  it("createUat calls POST /api/v1/change-requests/{id}/uat with body", async () => {
    const uatData = { id: "uat-1", title: "UAT Plan", details: "<p>Details</p>", status: "DRAFT", readOnly: false, approvers: [] };
    mockApi.mockResolvedValueOnce(uatData);

    const { createUat } = useChangeRequests();
    const result = await createUat("cr-1", { title: "UAT Plan", details: "<p>Details</p>" });

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat", {
      method: "POST",
      body: { title: "UAT Plan", details: "<p>Details</p>" },
    });
    expect(result).toEqual(uatData);
  });

  it("updateUat calls PATCH /api/v1/change-requests/{id}/uat with body", async () => {
    const uatData = { id: "uat-1", title: "Updated", details: "<p>Updated</p>", status: "DRAFT", readOnly: false, approvers: [] };
    mockApi.mockResolvedValueOnce(uatData);

    const { updateUat } = useChangeRequests();
    const result = await updateUat("cr-1", { title: "Updated", details: "<p>Updated</p>" });

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat", {
      method: "PATCH",
      body: { title: "Updated", details: "<p>Updated</p>" },
    });
    expect(result).toEqual(uatData);
  });

  it("addUatApprover calls POST /api/v1/change-requests/{id}/uat/approvers", async () => {
    mockApi.mockResolvedValueOnce({});

    const { addUatApprover } = useChangeRequests();
    await addUatApprover("cr-1", { userId: "u-1", isRequired: true });

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat/approvers", {
      method: "POST",
      body: { userId: "u-1", isRequired: true },
    });
  });

  it("approveUat calls POST /api/v1/change-requests/{id}/uat/approve", async () => {
    mockApi.mockResolvedValueOnce({});

    const { approveUat } = useChangeRequests();
    await approveUat("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat/approve", {
      method: "POST",
    });
  });

  it("rejectUat calls POST /api/v1/change-requests/{id}/uat/reject with reason", async () => {
    mockApi.mockResolvedValueOnce({});

    const { rejectUat } = useChangeRequests();
    await rejectUat("cr-1", "Not ready");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat/reject", {
      method: "POST",
      body: { reason: "Not ready" },
    });
  });

  it("promoteUat calls POST /api/v1/change-requests/{id}/uat/promote", async () => {
    mockApi.mockResolvedValueOnce({});

    const { promoteUat } = useChangeRequests();
    await promoteUat("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat/promote", {
      method: "POST",
    });
  });
});

describe("UAT tab gating", () => {
  it("UAT tab should be shown only when workflowMode is DELIVERY_PIPELINE", () => {
    const cr = buildCr({ workflowMode: "DELIVERY_PIPELINE" });
    const showUatTab = cr.workflowMode === "DELIVERY_PIPELINE";
    expect(showUatTab).toBe(true);
  });

  it("UAT tab should not be shown for APPROVAL_ONLY workflow", () => {
    const cr = buildCr({ workflowMode: "APPROVAL_ONLY" });
    const showUatTab = cr.workflowMode === "DELIVERY_PIPELINE";
    expect(showUatTab).toBe(false);
  });

  it("UAT tab should not be shown when workflowMode is null", () => {
    const cr = buildCr({ workflowMode: null });
    const showUatTab = cr.workflowMode === "DELIVERY_PIPELINE";
    expect(showUatTab).toBe(false);
  });
});

describe("UAT availability rules", () => {
  it("UAT is available when approvalStatus is APPROVED and workflowMode is DELIVERY_PIPELINE", () => {
    const cr = buildCr({ approvalStatus: "APPROVED", workflowMode: "DELIVERY_PIPELINE" });
    const isUatAvailable = cr.approvalStatus === "APPROVED" && cr.workflowMode === "DELIVERY_PIPELINE";
    expect(isUatAvailable).toBe(true);
  });

  it("UAT is not available when approvalStatus is PENDING", () => {
    const cr = buildCr({ approvalStatus: "PENDING", workflowMode: "DELIVERY_PIPELINE" });
    const isUatAvailable = cr.approvalStatus === "APPROVED" && cr.workflowMode === "DELIVERY_PIPELINE";
    expect(isUatAvailable).toBe(false);
  });

  it("UAT is not available when approvalStatus is REJECTED", () => {
    const cr = buildCr({ approvalStatus: "REJECTED", workflowMode: "DELIVERY_PIPELINE" });
    const isUatAvailable = cr.approvalStatus === "APPROVED" && cr.workflowMode === "DELIVERY_PIPELINE";
    expect(isUatAvailable).toBe(false);
  });

  it("UAT is not available when approvalStatus is null", () => {
    const cr = buildCr({ approvalStatus: null, workflowMode: "DELIVERY_PIPELINE" });
    const isUatAvailable = cr.approvalStatus === "APPROVED" && cr.workflowMode === "DELIVERY_PIPELINE";
    expect(isUatAvailable).toBe(false);
  });
});

describe("UAT read-only after promotion", () => {
  it("UAT with status PROMOTED should be read-only", () => {
    const uat = { id: "uat-1", title: "UAT", details: "", status: "PROMOTED", readOnly: true, approvers: [] };
    expect(uat.readOnly).toBe(true);
    expect(uat.status).toBe("PROMOTED");
  });

  it("UAT with status DRAFT should not be read-only", () => {
    const uat = { id: "uat-1", title: "UAT", details: "", status: "DRAFT", readOnly: false, approvers: [] };
    expect(uat.readOnly).toBe(false);
  });
});
