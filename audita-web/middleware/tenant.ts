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
export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore();
  const { ssrContext } = useNuxtApp();

  // Determine host: server-side from SSR context, client-side from window.
  const host = import.meta.server
    ? (ssrContext?.event.node.req.headers.host ?? "")
    : window.location.hostname;

  // Strip port if present (e.g. "acme.localhost:3000" → "acme.localhost")
  const hostname = host.split(":")[0];

  // Extract slug from the first subdomain segment.
  // "acme.audita.io"  → ["acme", "audita", "io"]  → "acme"
  // "localhost"       → ["localhost"]               → no subdomain
  const parts = hostname.split(".");
  const subdomain = parts.length >= 3 ? parts[0] : null;

  // Reject generic subdomains that are not tenant identifiers.
  const reservedSubdomains = new Set(["www", "app", "api", "mail", "smtp"]);
  const slugFromSubdomain =
    subdomain && !reservedSubdomains.has(subdomain) ? subdomain : null;

  // Fallback: ?tenant= query param — only respected in dev environments
  // (i.e. when there is no real subdomain) to prevent client-side bypass.
  const slugFromQuery =
    !slugFromSubdomain && import.meta.dev
      ? ((to.query.tenant as string | undefined) ?? null)
      : null;

  const resolved = slugFromSubdomain ?? slugFromQuery;

  if (resolved) {
    auth.tenantSlug = resolved;
  }
});
