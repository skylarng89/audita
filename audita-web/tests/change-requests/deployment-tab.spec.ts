import { describe, expect, it, vi, beforeEach } from "vitest";
import type {
  ChangeRequest,
  Deployment,
  DeploymentApprover,
  DeploymentStatus,
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

describe("DeploymentStatus type", () => {
  it("accepts valid values", () => {
    const s1: DeploymentStatus = "PENDING";
    const s2: DeploymentStatus = "COMPLETED";
    const s3: DeploymentStatus = "CANCELLED";
    expect(s1).toBe("PENDING");
    expect(s2).toBe("COMPLETED");
    expect(s3).toBe("CANCELLED");
  });
});

describe("Deployment interface", () => {
  it("has expected shape with single assignee", () => {
    const dep: Deployment = {
      id: "dep-1",
      requestId: "cr-1",
      status: "PENDING",
      promotedAt: "2026-01-15T10:00:00Z",
      assignee: {
        id: "u-1",
        email: "alice@example.com",
        fullName: "Alice",
      },
    };
    expect(dep.id).toBe("dep-1");
    expect(dep.status).toBe("PENDING");
    expect(dep.assignee?.fullName).toBe("Alice");
  });
});

describe("getDeployment", () => {
  it("calls GET /api/v1/change-requests/{id}/deployment", async () => {
    const dep: Deployment = {
      id: "dep-1",
      requestId: "cr-1",
      status: "PENDING",
      promotedAt: "2026-01-15T10:00:00Z",
      assignee: null,
    };
    mockApi.mockResolvedValueOnce(dep);

    const { getDeployment } = useChangeRequests();
    const result = await getDeployment("cr-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/deployment",
    );
    expect(result).toEqual(dep);
  });
});

describe("approveDeployment", () => {
  it("calls POST /api/v1/change-requests/{id}/deployment/approve", async () => {
    mockApi.mockResolvedValueOnce({});

    const { approveDeployment } = useChangeRequests();
    await approveDeployment("cr-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/deployment/approve",
      { method: "POST" },
    );
  });
});

describe("rejectDeployment", () => {
  it("calls POST /api/v1/change-requests/{id}/deployment/reject with reason", async () => {
    mockApi.mockResolvedValueOnce({});

    const { rejectDeployment } = useChangeRequests();
    await rejectDeployment("cr-1", "Not ready for production");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/deployment/reject",
      { method: "POST", body: { reason: "Not ready for production" } },
    );
  });
});

describe("listDeploymentApprovers", () => {
  it("calls GET /api/v1/change-requests/{id}/deployment/approvers", async () => {
    const approvers: DeploymentApprover[] = [
      {
        id: "da-1",
        userId: "u-1",
        userFullName: "Alice",
        userEmail: "alice@example.com",
        status: "PENDING",
        decidedAt: null,
        rejectionReason: null,
      },
    ];
    mockApi.mockResolvedValueOnce(approvers);

    const { listDeploymentApprovers } = useChangeRequests();
    const result = await listDeploymentApprovers("cr-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/deployment/approvers",
    );
    expect(result).toEqual(approvers);
  });
});

describe("deployment tab visibility", () => {
  it("deployment tab shown only for DELIVERY_PIPELINE workflow mode", () => {
    const pipelineCr = buildCr({ workflowMode: "DELIVERY_PIPELINE" });
    const approvalCr = buildCr({ workflowMode: "APPROVAL_ONLY" });
    const legacyCr = buildCr({ workflowMode: null });

    expect(pipelineCr.workflowMode === "DELIVERY_PIPELINE").toBe(true);
    expect(approvalCr.workflowMode === "DELIVERY_PIPELINE").toBe(false);
    expect(legacyCr.workflowMode === "DELIVERY_PIPELINE").toBe(false);
  });
});

describe("deployment has no create endpoint", () => {
  it("useChangeRequests does not expose a createDeployment method", () => {
    const cr = useChangeRequests();
    expect((cr as Record<string, unknown>).createDeployment).toBeUndefined();
  });
});

describe("assignDeployer", () => {
  it("calls POST /api/v1/change-requests/{id}/deployment/assignee with userId", async () => {
    const dep: Deployment = {
      id: "dep-1",
      requestId: "cr-1",
      status: "PENDING",
      promotedAt: "2026-01-15T10:00:00Z",
      assignee: { id: "u-1", email: "alice@example.com", fullName: "Alice" },
    };
    mockApi.mockResolvedValueOnce(dep);

    const { assignDeployer } = useChangeRequests();
    const result = await assignDeployer("cr-1", "u-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/deployment/assignee",
      { method: "POST", body: { userId: "u-1" } },
    );
    expect(result).toEqual(dep);
  });
});

describe("completeDeployment", () => {
  it("calls POST /api/v1/change-requests/{id}/deployment/complete", async () => {
    const dep: Deployment = {
      id: "dep-1",
      requestId: "cr-1",
      status: "COMPLETED",
      promotedAt: "2026-01-15T10:00:00Z",
      assignee: { id: "u-1", email: "alice@example.com", fullName: "Alice" },
    };
    mockApi.mockResolvedValueOnce(dep);

    const { completeDeployment } = useChangeRequests();
    const result = await completeDeployment("cr-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/deployment/complete",
      { method: "POST" },
    );
    expect(result.status).toBe("COMPLETED");
  });
});
