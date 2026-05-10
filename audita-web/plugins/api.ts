import { useAuthStore } from "~/stores/auth";

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig();
  const auth = useAuthStore();
  const baseURL = import.meta.server
    ? config.apiInternalBase
    : config.public.apiBase;

  const $api = $fetch.create({
    baseURL,

    onRequest({ request, options }) {
      if (!auth.isAuthenticated) {
        auth.hydrateFromCookie();
      }

      let requestPath = "";
      if (typeof request === "string") {
        requestPath = request;
      } else if (request instanceof Request) {
        requestPath = request.url;
      }

      const isBootstrapEndpoint =
        requestPath.includes("/api/platform/v1/bootstrap") ||
        requestPath.includes("/api/platform/v1/setup");

      if (isBootstrapEndpoint) {
        // Bootstrap must remain anonymous and tenant-agnostic.
        options.headers = new Headers();
        return;
      }

      const headers = new Headers(options.headers);

      // Inject Bearer token on every authenticated request
      if (auth.accessToken) {
        headers.set("Authorization", `Bearer ${auth.accessToken}`);
      }

      // Inject tenant slug for routing
      if (auth.tenantSlug) {
        headers.set("X-Tenant-Slug", auth.tenantSlug);
      }

      options.headers = headers;
    },

    async onResponseError({ response }) {
      // Only treat 401 as an expired-token signal worth refreshing.
      // 403 responses are domain-level permission/business errors (e.g. UPLOAD_FAILED,
      // NOT_PERMITTED) — retrying with a fresh token would still return 403, and
      // the catch below would force an erroneous logout.
      if (response.status === 401 && auth.isAuthenticated) {
        // Attempt silent refresh via the HttpOnly cookie
        try {
          const refreshed = await $fetch<{ accessToken: string }>(
            "/api/v1/auth/refresh",
            {
              method: "POST",
              baseURL,
            },
          );
          auth.setAccessToken(refreshed.accessToken);
        } catch {
          // Refresh failed — force logout
          await auth.logout();
        }
      }
    },
  });

  return {
    provide: { api: $api },
  };
});
