import { describe, expect, it, vi, beforeEach } from "vitest";

const mockApi = vi.fn();
vi.mock("~/composables/useApi", () => ({
  useApi: () => mockApi,
}));

import { useChangeRequests } from "~/composables/useChangeRequests";

beforeEach(() => {
  mockApi.mockReset();
});

describe("UAT comment composable methods", () => {
  it("composable exposes listUatComments", () => {
    const cr = useChangeRequests();
    expect(typeof cr.listUatComments).toBe("function");
  });

  it("composable exposes postUatComment", () => {
    const cr = useChangeRequests();
    expect(typeof cr.postUatComment).toBe("function");
  });

  it("listUatComments calls GET /api/v1/change-requests/{id}/uat/comments", async () => {
    const comments = [{ id: "c-1", body: "Looks good", createdAt: "2026-01-01T00:00:00Z" }];
    mockApi.mockResolvedValueOnce(comments);

    const { listUatComments } = useChangeRequests();
    const result = await listUatComments("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat/comments");
    expect(result).toEqual(comments);
  });

  it("postUatComment calls POST /api/v1/change-requests/{id}/uat/comments with body", async () => {
    const comment = { id: "c-2", body: "New comment", createdAt: "2026-01-01T00:00:00Z" };
    mockApi.mockResolvedValueOnce(comment);

    const { postUatComment } = useChangeRequests();
    const result = await postUatComment("cr-1", "New comment");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/uat/comments", {
      method: "POST",
      body: { body: "New comment" },
    });
    expect(result).toEqual(comment);
  });
});

describe("Deployment comment composable methods", () => {
  it("composable exposes listDeploymentComments", () => {
    const cr = useChangeRequests();
    expect(typeof cr.listDeploymentComments).toBe("function");
  });

  it("composable exposes postDeploymentComment", () => {
    const cr = useChangeRequests();
    expect(typeof cr.postDeploymentComment).toBe("function");
  });

  it("listDeploymentComments calls GET /api/v1/change-requests/{id}/deployment/comments", async () => {
    const comments = [{ id: "c-1", body: "Approved", createdAt: "2026-01-01T00:00:00Z" }];
    mockApi.mockResolvedValueOnce(comments);

    const { listDeploymentComments } = useChangeRequests();
    const result = await listDeploymentComments("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/deployment/comments");
    expect(result).toEqual(comments);
  });

  it("postDeploymentComment calls POST /api/v1/change-requests/{id}/deployment/comments with body", async () => {
    const comment = { id: "c-2", body: "Deploy note", createdAt: "2026-01-01T00:00:00Z" };
    mockApi.mockResolvedValueOnce(comment);

    const { postDeploymentComment } = useChangeRequests();
    const result = await postDeploymentComment("cr-1", "Deploy note");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/deployment/comments", {
      method: "POST",
      body: { body: "Deploy note" },
    });
    expect(result).toEqual(comment);
  });
});
