import {
  clearServerSession,
  restoreSessionFromCookie,
} from "~/composables/sessionRestore";
import { resolveTenantSlug } from "~/composables/tenantResolution";
import { useAuthStore } from "~/stores/auth";

/**
 * Restores the in-memory access token from the HttpOnly refresh cookie before
 * the first render. This keeps session state out of JS-readable storage while
 * still allowing clean reload/redeploy recovery through the backend refresh flow.
 */
export default defineNuxtPlugin(async () => {
  const auth = useAuthStore();
  const isDev = Boolean(import.meta.dev);
  const config = useRuntimeConfig();
  const requestUrl = new URL(globalThis.location.href);
  const tenantSlug = resolveTenantSlug({
    hostname: requestUrl.host,
    queryTenant: requestUrl.searchParams.get("tenant"),
    isDev,
  });

  if (tenantSlug) {
    auth.setTenantSlug(tenantSlug);
  }

  try {
    await restoreSessionFromCookie(
      auth,
      config.public.apiContractVersion,
      false,
    );
  } catch {
    try {
      await clearServerSession();
    } catch {}
    auth.clearAuth();
    if (tenantSlug) {
      auth.setTenantSlug(tenantSlug);
    }
  } finally {
    auth.markSessionInitialized();
  }
});
