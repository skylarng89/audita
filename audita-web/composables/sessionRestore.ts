import {
  API_CONTRACT_HEADER,
  isApiContractCompatible,
} from "~/composables/apiContract";
import type { useAuthStore } from "~/stores/auth";
import type { AuthResponse } from "~/types";

type AuthStore = ReturnType<typeof useAuthStore>;

export class ApiContractMismatchError extends Error {
  constructor() {
    super("API contract version mismatch");
  }
}

export async function restoreSessionFromCookie(
  auth: AuthStore,
  expectedApiContractVersion: string | null,
  broadcast = false,
) {
  const headers = new Headers();
  if (auth.tenantSlug) {
    headers.set("X-Tenant-Slug", auth.tenantSlug);
  }

  const response = await $fetch.raw<AuthResponse>("/api/v1/auth/session", {
    method: "POST",
    headers,
  });

  const actualApiContractVersion = response.headers.get(API_CONTRACT_HEADER);
  if (
    !isApiContractCompatible(
      actualApiContractVersion,
      expectedApiContractVersion,
    )
  ) {
    throw new ApiContractMismatchError();
  }

  auth.setAuth(response._data, { broadcast });
  return response._data;
}

export async function clearServerSession() {
  await fetch("/api/v1/auth/logout", {
    credentials: "same-origin",
    method: "POST",
  });
}
