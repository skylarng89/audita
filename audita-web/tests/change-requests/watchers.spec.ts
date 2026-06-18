import { describe, expect, it, vi, beforeEach } from "vitest";

const mockApi = vi.fn();
vi.mock("~/composables/useApi", () => ({
  useApi: () => mockApi,
}));

import { useChangeRequests } from "~/composables/useChangeRequests";

beforeEach(() => {
  mockApi.mockReset();
});

describe("listWatchers", () => {
  it("calls GET /api/v1/change-requests/{id}/watchers", async () => {
    const watchers = [
      { id: "w-1", userId: "u-1", userEmail: "a@example.com", userFullName: "Alice", createdAt: "2026-01-01T00:00:00Z" },
    ];
    mockApi.mockResolvedValueOnce(watchers);

    const { listWatchers } = useChangeRequests();
    const result = await listWatchers("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/watchers");
    expect(result).toEqual(watchers);
  });
});

describe("addWatchers", () => {
  it("calls POST /api/v1/change-requests/{id}/watchers with userIds", async () => {
    mockApi.mockResolvedValueOnce([]);

    const { addWatchers } = useChangeRequests();
    await addWatchers("cr-1", ["u-1", "u-2"]);

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/watchers", {
      method: "POST",
      body: { userIds: ["u-1", "u-2"] },
    });
  });
});

describe("removeWatcher", () => {
  it("calls DELETE /api/v1/change-requests/{id}/watchers/{userId}", async () => {
    mockApi.mockResolvedValueOnce(undefined);

    const { removeWatcher } = useChangeRequests();
    await removeWatcher("cr-1", "u-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/watchers/u-1", {
      method: "DELETE",
    });
  });
});

describe("promoteWatcher", () => {
  it("calls POST /api/v1/change-requests/{id}/watchers/{userId}/promote", async () => {
    const approver = { id: "a-1", userId: "u-1", userEmail: "a@example.com", userFullName: "Alice", userRole: "Requester", position: 1, status: "PENDING", rejectionReason: null, decidedAt: null, isAdHoc: false };
    mockApi.mockResolvedValueOnce(approver);

    const { promoteWatcher } = useChangeRequests();
    const result = await promoteWatcher("cr-1", "u-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/watchers/u-1/promote",
      { method: "POST" },
    );
    expect(result.id).toBe("a-1");
  });
});

describe("demoteApprover", () => {
  it("calls POST /api/v1/change-requests/{id}/approvers/{approverId}/demote", async () => {
    const watcher = { id: "w-1", userId: "u-1", userEmail: "a@example.com", userFullName: "Alice", createdAt: "2026-01-01T00:00:00Z" };
    mockApi.mockResolvedValueOnce(watcher);

    const { demoteApprover } = useChangeRequests();
    const result = await demoteApprover("cr-1", "a-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/approvers/a-1/demote",
      { method: "POST" },
    );
    expect(result.id).toBe("w-1");
  });
});

describe("listUatWatchers", () => {
  it("calls GET /api/v1/change-requests/{id}/uat/watchers", async () => {
    mockApi.mockResolvedValueOnce([]);

    const { listUatWatchers } = useChangeRequests();
    await listUatWatchers("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat/watchers");
  });
});

describe("addUatWatchers", () => {
  it("calls POST /api/v1/change-requests/{id}/uat/watchers with userIds", async () => {
    mockApi.mockResolvedValueOnce([]);

    const { addUatWatchers } = useChangeRequests();
    await addUatWatchers("cr-1", ["u-1"]);

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat/watchers", {
      method: "POST",
      body: { userIds: ["u-1"] },
    });
  });
});

describe("removeUatWatcher", () => {
  it("calls DELETE /api/v1/change-requests/{id}/uat/watchers/{userId}", async () => {
    mockApi.mockResolvedValueOnce(undefined);

    const { removeUatWatcher } = useChangeRequests();
    await removeUatWatcher("cr-1", "u-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/uat/watchers/u-1",
      { method: "DELETE" },
    );
  });
});

describe("promoteUatWatcher", () => {
  it("calls POST /api/v1/change-requests/{id}/uat/watchers/{userId}/promote", async () => {
    mockApi.mockResolvedValueOnce({});

    const { promoteUatWatcher } = useChangeRequests();
    await promoteUatWatcher("cr-1", "u-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/uat/watchers/u-1/promote",
      { method: "POST" },
    );
  });
});

describe("demoteUatApprover", () => {
  it("calls POST /api/v1/change-requests/{id}/uat/approvers/{approverId}/demote", async () => {
    mockApi.mockResolvedValueOnce({});

    const { demoteUatApprover } = useChangeRequests();
    await demoteUatApprover("cr-1", "a-1");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/uat/approvers/a-1/demote",
      { method: "POST" },
    );
  });
});
