import {
  API_CONTRACT_HEADER,
  isApiContractCompatible,
} from "~/composables/apiContract";
import { describe, expect, it } from "vitest";

describe("api contract helpers", () => {
  it("uses a stable response header name", () => {
    expect(API_CONTRACT_HEADER).toBe("X-Audita-Api-Contract");
  });

  it("accepts matching contract versions", () => {
    expect(
      isApiContractCompatible("2026-05-12-auth-v2", "2026-05-12-auth-v2"),
    ).toBe(true);
  });

  it("rejects mismatched contract versions", () => {
    expect(
      isApiContractCompatible("2026-05-12-auth-v1", "2026-05-12-auth-v2"),
    ).toBe(false);
  });
});
