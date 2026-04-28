import { useAuthStore } from "~/stores/auth";

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig();
  const auth = useAuthStore();
  const baseURL = import.meta.server
    ? (config.apiInternalBase as string)
    : (config.public.apiBase as string);

  const $api = $fetch.create({
    baseURL,

    onRequest({ options }) {
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
          auth.accessToken = refreshed.accessToken;
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
