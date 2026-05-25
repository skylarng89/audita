import { useAuthStore } from "~/stores/auth";
import { resolveTenantSlug } from "~/composables/tenantResolution";

// Resolves the active tenant slug and makes it available via the auth store.
//
// Resolution order:
//   1. Stored slug (from login response) — authoritative
//   2. Subdomain — acme.audita.io  →  "acme"   (pre-login fallback)
//   3. Query param ?tenant=acme    →  "acme"   (localhost dev only)
//
// The backend resolves the final tenant via X-Forwarded-Host subdomain
// mapping, so the frontend slug does not need to match the database slug
// exactly. After login, the auth store holds the server-returned slug.
export default defineNuxtRouteMiddleware(async (to) => {
  const auth = useAuthStore();
  const { ssrContext } = useNuxtApp();
  const isDev = Boolean(import.meta.dev);

  // If already authenticated with a server-returned slug, trust it.
  // The backend resolved the correct tenant via X-Forwarded-Host.
  if (auth.isAuthenticated && auth.tenantSlug) {
    return;
  }

  // Pre-login: resolve slug from subdomain so the login request
  // includes X-Tenant-Slug. The backend may resolve a different tenant
  // via X-Forwarded-Host, but the header still helps for direct API calls.
  const browserHostname =
    globalThis.location?.hostname ??
    globalThis.window?.location?.hostname ??
    "";
  const host = import.meta.server
    ? (ssrContext?.event.node.req.headers.host ?? "")
    : browserHostname;
  const resolved = resolveTenantSlug({
    hostname: host,
    queryTenant: (to.query.tenant as string | undefined) ?? null,
    isDev,
  });

  if (resolved) {
    auth.setTenantSlug(resolved);
  }
});