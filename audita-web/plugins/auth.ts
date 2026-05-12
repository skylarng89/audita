import { resolveTenantSlug } from "~/composables/tenantResolution";
import { useAuthStore } from "~/stores/auth";
import type { AuthResponse } from "~/types";

/**
 * Restores the in-memory access token from the HttpOnly refresh cookie before
 * the first render. This keeps session state out of JS-readable storage while
 * still allowing clean reload/redeploy recovery through the backend refresh flow.
 */
export default defineNuxtPlugin(async () => {
  const auth = useAuthStore();
  const isDev = Boolean(import.meta.dev);
  const requestUrl = import.meta.server
    ? useRequestURL()
    : new URL(window.location.href);
  const tenantSlug = resolveTenantSlug({
    hostname: requestUrl.host,
    queryTenant: requestUrl.searchParams.get("tenant"),
    isDev,
  });

  if (tenantSlug) {
    auth.setTenantSlug(tenantSlug);
  }

  const headers = new Headers();
  if (auth.tenantSlug) {
    headers.set("X-Tenant-Slug", auth.tenantSlug);
  }

  try {
    const refreshed = import.meta.server
      ? await useRequestFetch()<AuthResponse>("/api/v1/auth/refresh", {
          method: "POST",
          headers,
        })
      : await $fetch<AuthResponse>("/api/v1/auth/refresh", {
          method: "POST",
          headers,
        });
    auth.setAuth(refreshed);
  } catch {
    auth.clearAuth();
    if (tenantSlug) {
      auth.setTenantSlug(tenantSlug);
    }
  } finally {
    auth.markSessionInitialized();
  }
});
