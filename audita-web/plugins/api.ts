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
      } else {
        requestPath = request.toString();
      }

      const isBootstrapEndpoint =
        requestPath.includes("/api/platform/v1/bootstrap") ||
        requestPath.includes("/api/platform/v1/setup");

      if (isBootstrapEndpoint) {
        // Bootstrap must remain anonymous and tenant-agnostic.
        options.headers = {};
        return;
      }

      // Inject Bearer token on every authenticated request
      if (auth.accessToken) {
        options.headers = {
          ...options.headers,
          Authorization: `Bearer ${auth.accessToken}`,
        };
      }

      // Inject tenant slug for routing
      if (auth.tenantSlug) {
        options.headers = {
          ...(options.headers as Record<string, string>),
          "X-Tenant-Slug": auth.tenantSlug,
        };
      }
    },

    async onResponseError({ response }) {
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
