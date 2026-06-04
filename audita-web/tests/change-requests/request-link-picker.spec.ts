import { describe, expect, it, vi, beforeEach } from "vitest";

const mockApi = vi.fn();
vi.mock("~/composables/useApi", () => ({
  useApi: () => mockApi,
}));

import { useChangeRequests } from "~/composables/useChangeRequests";

beforeEach(() => {
  mockApi.mockReset();
});

describe("searchRequests", () => {
  it("calls GET /api/v1/change-requests/search with query and limit", async () => {
    const results = [
      { id: "cr-1", displayId: "CR-001", title: "Migration", status: "DRAFT" },
    ];
    mockApi.mockResolvedValueOnce(results);

    const { searchRequests } = useChangeRequests();
    const result = await searchRequests("migr", 5);

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/search",
      { query: { query: "migr", limit: 5 } },
    );
    expect(result).toEqual(results);
  });

  it("defaults limit to 10", async () => {
    mockApi.mockResolvedValueOnce([]);

    const { searchRequests } = useChangeRequests();
    await searchRequests("test");

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/search",
      { query: { query: "test", limit: 10 } },
    );
  });
});

describe("getLinkedRequests", () => {
  it("calls GET /api/v1/change-requests/{id}/links", async () => {
    mockApi.mockResolvedValueOnce(["cr-2", "cr-3"]);

    const { getLinkedRequests } = useChangeRequests();
    const result = await getLinkedRequests("cr-1");

    expect(mockApi).toHaveBeenCalledWith("/api/v1/change-requests/cr-1/links");
    expect(result).toEqual(["cr-2", "cr-3"]);
  });
});

describe("upsertLinks", () => {
  it("calls PUT /api/v1/change-requests/{id}/links with linkedRequestIds", async () => {
    mockApi.mockResolvedValueOnce(undefined);

    const { upsertLinks } = useChangeRequests();
    await upsertLinks("cr-1", ["cr-2", "cr-3"]);

    expect(mockApi).toHaveBeenCalledWith(
      "/api/v1/change-requests/cr-1/links",
      { method: "PUT", body: { linkedRequestIds: ["cr-2", "cr-3"] } },
    );
  });
});
