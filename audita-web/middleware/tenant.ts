import { useAuthStore } from "~/stores/auth";
import { resolveTenantSlug } from "~/composables/tenantResolution";

// Resolves the active tenant slug from the request host and makes it
// available to the API plugin via the auth store.
//
// Resolution order:
//   1. Subdomain — acme.audita.io  →  "acme"
//   2. Query param ?tenant=acme    →  "acme"  (localhost dev only)
//
// The resolved slug is written to auth.tenantSlug so that plugins/api.ts
// can inject X-Tenant-Slug on every outgoing request without duplicating
// the resolution logic.
export default defineNuxtRouteMiddleware(async (to) => {
  const auth = useAuthStore();
  const { ssrContext } = useNuxtApp();
  const isDev = Boolean(import.meta.dev);

  // Determine host: server-side from SSR context, client-side from window.
  const host = import.meta.server
    ? (ssrContext?.event.node.req.headers.host ?? "")
    : window.location.hostname;
  const resolved = resolveTenantSlug({
    hostname: host,
    queryTenant: (to.query.tenant as string | undefined) ?? null,
    isDev,
  });

  if (
    resolved &&
    auth.isAuthenticated &&
    auth.tenantSlug &&
    resolved !== auth.tenantSlug
  ) {
    await auth.logout();
    return;
  }

  if (resolved) {
    auth.setTenantSlug(resolved);
  }
});
