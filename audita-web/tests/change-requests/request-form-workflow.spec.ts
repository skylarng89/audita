import { describe, expect, it, vi, beforeEach } from "vitest";
import type {
  ChangeRequest,
  Department,
  WorkflowMode,
  CompletionStatus,
} from "~/types";

const mockApi = vi.fn();
vi.mock("~/composables/useApi", () => ({
  useApi: () => mockApi,
}));

import { useChangeRequests } from "~/composables/useChangeRequests";

beforeEach(() => {
  mockApi.mockReset();
});

describe("listActiveDepartments", () => {
  it("calls GET /api/v1/settings/departments/active", async () => {
    const departments: Department[] = [
      { id: "d1", name: "Engineering", code: "ENG", isActive: true, displayOrder: 1 },
      { id: "d2", name: "Operations", code: "OPS", isActive: true, displayOrder: 2 },
    ];
    mockApi.mockResolvedValueOnce(departments);

    const { listActiveDepartments } = useChangeRequests();
    const result = await listActiveDepartments();

    expect(mockApi).toHaveBeenCalledWith("/api/v1/settings/departments/active");
    expect(result).toEqual(departments);
  });
});

describe("create payload includes workflow fields", () => {
  it("sends workflowMode, requestDepartmentId, and destinationDepartmentId", async () => {
    mockApi.mockResolvedValueOnce({ id: "cr-1" });

    const { create } = useChangeRequests();
    await create({
      title: "Test CR",
      priority: "HIGH",
      riskLevel: "MEDIUM",
      approvalType: "LINEAR",
      workflowMode: "DELIVERY_PIPELINE",
      requestDepartmentId: "d1",
      destinationDepartmentId: "d2",
    });

    const callBody = mockApi.mock.calls[0]![1]!.body as Record<string, unknown>;
    expect(callBody.workflowMode).toBe("DELIVERY_PIPELINE");
    expect(callBody.requestDepartmentId).toBe("d1");
    expect(callBody.destinationDepartmentId).toBe("d2");
  });
});

describe("update payload includes workflow fields", () => {
  it("sends workflowMode and department IDs on PATCH", async () => {
    mockApi.mockResolvedValueOnce({ id: "cr-1" });

    const { update } = useChangeRequests();
    await update("cr-1", {
      title: "Updated",
      workflowMode: "APPROVAL_ONLY",
      requestDepartmentId: "d1",
      destinationDepartmentId: null,
    });

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1",
      expect.objectContaining({ method: "PATCH" }),
    );
    const callBody = mockApi.mock.calls[0]![1]!.body as Record<string, unknown>;
    expect(callBody.workflowMode).toBe("APPROVAL_ONLY");
    expect(callBody.requestDepartmentId).toBe("d1");
    expect(callBody.destinationDepartmentId).toBeNull();
  });
});

describe("WorkflowMode type", () => {
  it("accepts valid values", () => {
    const mode1: WorkflowMode = "APPROVAL_ONLY";
    const mode2: WorkflowMode = "DELIVERY_PIPELINE";
    expect(mode1).toBe("APPROVAL_ONLY");
    expect(mode2).toBe("DELIVERY_PIPELINE");
  });
});

describe("CompletionStatus type", () => {
  it("accepts valid values", () => {
    const s1: CompletionStatus = "IN_PROGRESS";
    const s2: CompletionStatus = "COMPLETED";
    expect(s1).toBe("IN_PROGRESS");
    expect(s2).toBe("COMPLETED");
  });
});

describe("ChangeRequest workflow fields", () => {
  it("includes workflowMode, department IDs, displayId, approvalStatus, completionStatus", () => {
    const cr: ChangeRequest = {
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
      requestDepartmentId: "d1",
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
    };

    expect(cr.workflowMode).toBe("APPROVAL_ONLY");
    expect(cr.requestDepartmentId).toBe("d1");
    expect(cr.destinationDepartmentId).toBeNull();
    expect(cr.displayId).toBe("CR-0001");
    expect(cr.approvalStatus).toBe("PENDING");
    expect(cr.completionStatus).toBe("IN_PROGRESS");
  });

  it("allows null workflowMode for legacy requests", () => {
    const cr: ChangeRequest = {
      id: "cr-legacy",
      displayId: "CR-0000",
      title: "Legacy",
      description: null,
      priority: "LOW",
      riskLevel: "LOW",
      category: null,
      status: "DRAFT",
      approvalType: "LINEAR",
      approvalStatus: null,
      completionStatus: null,
      approvalLocked: false,
      workflowMode: null,
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
    };

    expect(cr.workflowMode).toBeNull();
    expect(cr.requestDepartmentId).toBeNull();
    expect(cr.destinationDepartmentId).toBeNull();
  });
});

describe("Department interface", () => {
  it("has expected shape", () => {
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

  it("allows null code", () => {
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
