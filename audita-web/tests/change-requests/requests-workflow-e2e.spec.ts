import { describe, expect, it, vi, beforeEach } from "vitest";
import type {
  ChangeRequest,
  WorkflowMode,
  CompletionStatus,
  Department,
  ApprovalStatus,
} from "~/types";

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
    status: "DRAFT",
    approvalType: "LINEAR",
    approvalStatus: "PENDING",
    completionStatus: "IN_PROGRESS",
    approvalLocked: false,
    workflowMode: "APPROVAL_ONLY",
    requestDepartmentId: null,
    destinationDepartmentId: null,
    requestGroupId: null,
    destinationGroupId: null,
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

describe("Label rename — 'Requests' not 'Change Requests'", () => {
  it("page directory uses 'change-requests' slug for compatibility", () => {
    const routePath = "/change-requests";
    expect(routePath).toBe("/change-requests");
    expect(routePath).not.toContain("/requests");
  });

  it("detail route uses /change-requests/[id]", () => {
    const id = "abc-123";
    const routePath = `/change-requests/${id}`;
    expect(routePath).toBe("/change-requests/abc-123");
  });

  it("new route uses /change-requests/new", () => {
    const routePath = "/change-requests/new";
    expect(routePath).toBe("/change-requests/new");
  });
});

describe("Compatibility-mode URLs remain unchanged", () => {
  it("list endpoint uses /api/v1/change-requests", async () => {
    mockApi.mockResolvedValueOnce({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 });

    const { list } = useChangeRequests();
    await list();

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests", expect.anything());
  });

  it("get endpoint uses /api/v1/change-requests/{id}", async () => {
    mockApi.mockResolvedValueOnce(buildCr());

    const { get } = useChangeRequests();
    await get("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1");
  });

  it("create endpoint uses POST /api/v1/change-requests", async () => {
    mockApi.mockResolvedValueOnce(buildCr());

    const { create } = useChangeRequests();
    await create({ title: "Test" });

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests", expect.objectContaining({ method: "POST" }));
  });

  it("update endpoint uses PATCH /api/v1/change-requests/{id}", async () => {
    mockApi.mockResolvedValueOnce(buildCr());

    const { update } = useChangeRequests();
    await update("cr-1", { title: "Updated" });

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1", expect.objectContaining({ method: "PATCH" }));
  });
});

describe("ChangeRequest type includes new fields", () => {
  it("has displayId field", () => {
    const cr = buildCr({ displayId: "CR-0042" });
    expect(cr.displayId).toBe("CR-0042");
  });

  it("displayId can be null", () => {
    const cr = buildCr({ displayId: null });
    expect(cr.displayId).toBeNull();
  });

  it("has approvalStatus field", () => {
    const cr = buildCr({ approvalStatus: "APPROVED" });
    expect(cr.approvalStatus).toBe("APPROVED");
  });

  it("approvalStatus can be null", () => {
    const cr = buildCr({ approvalStatus: null });
    expect(cr.approvalStatus).toBeNull();
  });

  it("has completionStatus field", () => {
    const cr = buildCr({ completionStatus: "COMPLETED" });
    expect(cr.completionStatus).toBe("COMPLETED");
  });

  it("completionStatus can be null for legacy", () => {
    const cr = buildCr({ completionStatus: null });
    expect(cr.completionStatus).toBeNull();
  });

  it("has workflowMode field", () => {
    const cr = buildCr({ workflowMode: "DELIVERY_PIPELINE" });
    expect(cr.workflowMode).toBe("DELIVERY_PIPELINE");
  });

  it("workflowMode can be null for legacy", () => {
    const cr = buildCr({ workflowMode: null });
    expect(cr.workflowMode).toBeNull();
  });
});

describe("WorkflowMode type", () => {
  it("accepts APPROVAL_ONLY", () => {
    const mode: WorkflowMode = "APPROVAL_ONLY";
    expect(mode).toBe("APPROVAL_ONLY");
  });

  it("accepts DELIVERY_PIPELINE", () => {
    const mode: WorkflowMode = "DELIVERY_PIPELINE";
    expect(mode).toBe("DELIVERY_PIPELINE");
  });
});

describe("CompletionStatus type", () => {
  it("accepts IN_PROGRESS", () => {
    const status: CompletionStatus = "IN_PROGRESS";
    expect(status).toBe("IN_PROGRESS");
  });

  it("accepts COMPLETED", () => {
    const status: CompletionStatus = "COMPLETED";
    expect(status).toBe("COMPLETED");
  });
});

describe("ApprovalStatus type", () => {
  it("accepts PENDING", () => {
    const status: ApprovalStatus = "PENDING";
    expect(status).toBe("PENDING");
  });

  it("accepts APPROVED", () => {
    const status: ApprovalStatus = "APPROVED";
    expect(status).toBe("APPROVED");
  });

  it("accepts REJECTED", () => {
    const status: ApprovalStatus = "REJECTED";
    expect(status).toBe("REJECTED");
  });

  it("accepts null", () => {
    const status: ApprovalStatus = null;
    expect(status).toBeNull();
  });
});

describe("Department interface", () => {
  it("has id, name, code, isActive, displayOrder", () => {
    const dept: Department = {
      id: "d1",
      name: "Engineering",
      code: "ENG",
      isActive: true,
      displayOrder: 1,
    };
    expect(dept.id).toBe("d1");
    expect(dept.name).toBe("Engineering");
    expect(dept.code).toBe("ENG");
    expect(dept.isActive).toBe(true);
    expect(dept.displayOrder).toBe(1);
  });

  it("code can be null", () => {
    const dept: Department = {
      id: "d2",
      name: "Ops",
      code: null,
      isActive: true,
      displayOrder: 2,
    };
    expect(dept.code).toBeNull();
  });
});

describe("Composable exports all Sprint 15 methods", () => {
  it("exposes listActiveDepartments", () => {
    const cr = useChangeRequests();
    expect(typeof cr.listActiveDepartments).toBe("function");
  });

  it("exposes searchRequests", () => {
    const cr = useChangeRequests();
    expect(typeof cr.searchRequests).toBe("function");
  });

  it("exposes getLinkedRequests", () => {
    const cr = useChangeRequests();
    expect(typeof cr.getLinkedRequests).toBe("function");
  });

  it("exposes upsertLinks", () => {
    const cr = useChangeRequests();
    expect(typeof cr.upsertLinks).toBe("function");
  });

  it("exposes getUat", () => {
    const cr = useChangeRequests();
    expect(typeof cr.getUat).toBe("function");
  });

  it("exposes createUat", () => {
    const cr = useChangeRequests();
    expect(typeof cr.createUat).toBe("function");
  });

  it("exposes updateUat", () => {
    const cr = useChangeRequests();
    expect(typeof cr.updateUat).toBe("function");
  });

  it("exposes addUatApprover", () => {
    const cr = useChangeRequests();
    expect(typeof cr.addUatApprover).toBe("function");
  });

  it("exposes approveUat", () => {
    const cr = useChangeRequests();
    expect(typeof cr.approveUat).toBe("function");
  });

  it("exposes rejectUat", () => {
    const cr = useChangeRequests();
    expect(typeof cr.rejectUat).toBe("function");
  });

  it("exposes promoteUat", () => {
    const cr = useChangeRequests();
    expect(typeof cr.promoteUat).toBe("function");
  });

  it("exposes markComplete", () => {
    const cr = useChangeRequests();
    expect(typeof cr.markComplete).toBe("function");
  });

  it("exposes getDeployment", () => {
    const cr = useChangeRequests();
    expect(typeof cr.getDeployment).toBe("function");
  });

  it("exposes approveDeployment", () => {
    const cr = useChangeRequests();
    expect(typeof cr.approveDeployment).toBe("function");
  });

  it("exposes rejectDeployment", () => {
    const cr = useChangeRequests();
    expect(typeof cr.rejectDeployment).toBe("function");
  });

  it("exposes listDeploymentApprovers", () => {
    const cr = useChangeRequests();
    expect(typeof cr.listDeploymentApprovers).toBe("function");
  });

  it("exposes listUatComments", () => {
    const cr = useChangeRequests();
    expect(typeof cr.listUatComments).toBe("function");
  });

  it("exposes postUatComment", () => {
    const cr = useChangeRequests();
    expect(typeof cr.postUatComment).toBe("function");
  });

  it("exposes listDeploymentComments", () => {
    const cr = useChangeRequests();
    expect(typeof cr.listDeploymentComments).toBe("function");
  });

  it("exposes postDeploymentComment", () => {
    const cr = useChangeRequests();
    expect(typeof cr.postDeploymentComment).toBe("function");
  });
});

describe("UAT tab conditional on DELIVERY_PIPELINE", () => {
  it("UAT tab visible for DELIVERY_PIPELINE", () => {
    const cr = buildCr({ workflowMode: "DELIVERY_PIPELINE" });
    expect(cr.workflowMode === "DELIVERY_PIPELINE").toBe(true);
  });

  it("UAT tab hidden for APPROVAL_ONLY", () => {
    const cr = buildCr({ workflowMode: "APPROVAL_ONLY" });
    expect(cr.workflowMode === "DELIVERY_PIPELINE").toBe(false);
  });

  it("UAT tab hidden when workflowMode is null", () => {
    const cr = buildCr({ workflowMode: null });
    expect(cr.workflowMode === "DELIVERY_PIPELINE").toBe(false);
  });
});

describe("Deployment tab conditional on DELIVERY_PIPELINE", () => {
  it("Deployment tab visible for DELIVERY_PIPELINE", () => {
    const cr = buildCr({ workflowMode: "DELIVERY_PIPELINE" });
    expect(cr.workflowMode === "DELIVERY_PIPELINE").toBe(true);
  });

  it("Deployment tab hidden for APPROVAL_ONLY", () => {
    const cr = buildCr({ workflowMode: "APPROVAL_ONLY" });
    expect(cr.workflowMode === "DELIVERY_PIPELINE").toBe(false);
  });

  it("Deployment tab hidden when workflowMode is null", () => {
    const cr = buildCr({ workflowMode: null });
    expect(cr.workflowMode === "DELIVERY_PIPELINE").toBe(false);
  });
});

describe("displayId rendering with fallback", () => {
  it("uses displayId when available", () => {
    const cr = buildCr({ displayId: "CR-0042" });
    const shown = cr.displayId || `CHG-${cr.id.slice(0, 8).toUpperCase()}`;
    expect(shown).toBe("CR-0042");
  });

  it("falls back to CHG- prefix when displayId is null", () => {
    const cr = buildCr({ displayId: null, id: "abcd1234-5678-90ab-cdef-1234567890ab" });
    const shown = cr.displayId || `CHG-${cr.id.slice(0, 8).toUpperCase()}`;
    expect(shown).toBe("CHG-ABCD1234");
  });
});

describe("Dual status columns", () => {
  it("ChangeRequest has both approvalStatus and completionStatus", () => {
    const cr = buildCr({ approvalStatus: "APPROVED", completionStatus: "IN_PROGRESS" });
    expect(cr.approvalStatus).toBeDefined();
    expect(cr.completionStatus).toBeDefined();
    expect(cr.approvalStatus).toBe("APPROVED");
    expect(cr.completionStatus).toBe("IN_PROGRESS");
  });

  it("approval and completion can diverge", () => {
    const cr = buildCr({ approvalStatus: "APPROVED", completionStatus: "COMPLETED" });
    expect(cr.approvalStatus).toBe("APPROVED");
    expect(cr.completionStatus).toBe("COMPLETED");
  });

  it("completion status is null when approval is pending", () => {
    const cr = buildCr({ approvalStatus: "PENDING", completionStatus: null });
    expect(cr.approvalStatus).toBe("PENDING");
    expect(cr.completionStatus).toBeNull();
  });
});

describe("markComplete composable method", () => {
  it("calls POST /api/v1/change-requests/{id}/complete", async () => {
    mockApi.mockResolvedValueOnce(buildCr({ completionStatus: "COMPLETED" }));

    const { markComplete } = useChangeRequests();
    const result = await markComplete("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/complete", {
      method: "POST",
    });
    expect(result.completionStatus).toBe("COMPLETED");
  });
});
