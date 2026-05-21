import { resolveApiErrorMessage } from "~/composables/apiError";
import { describe, expect, it } from "vitest";

describe("api error message resolver", () => {
  it("returns mapped message when errorCode is known", () => {
    const error = {
      data: {
        errorCode: "APPROVERS_LOCKED",
        detail: "Approvers are locked and cannot be changed.",
      },
    };

    expect(resolveApiErrorMessage(error, "Fallback message")).toBe(
      "Approvers are locked for this change request.",
    );
  });

  it("uses detail for unknown errorCode", () => {
    const error = {
      data: {
        errorCode: "SOMETHING_NEW",
        detail: "Server returned an unknown domain rule.",
      },
    };

    expect(resolveApiErrorMessage(error, "Fallback message")).toBe(
      "Server returned an unknown domain rule.",
    );
  });

  it("uses title when detail is missing", () => {
    const error = {
      data: {
        title: "Action Not Permitted",
      },
    };

    expect(resolveApiErrorMessage(error, "Fallback message")).toBe(
      "Action Not Permitted",
    );
  });

  it("uses fallback when payload is missing", () => {
    expect(resolveApiErrorMessage(new Error("boom"), "Fallback message")).toBe(
      "Fallback message",
    );
  });
});
