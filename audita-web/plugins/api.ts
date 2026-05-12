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

    async onResponseError(ctx) {
      const { request, options, response } = ctx;

      let requestPath = "";
      if (typeof request === "string") {
        requestPath = request;
      } else if (request instanceof Request) {
        requestPath = request.url;
      }

      const isRefreshEndpoint = requestPath.includes("/api/v1/auth/refresh");
      const alreadyRetried = Boolean(
        (options as { _authRetry?: boolean })._authRetry,
      );
      const isEmptyForbidden =
        response.status === 403 && response._data == null;
      const shouldAttemptRefresh =
        auth.isAuthenticated &&
        !isRefreshEndpoint &&
        !alreadyRetried &&
        (response.status === 401 || isEmptyForbidden);

      if (!shouldAttemptRefresh) {
        return;
      }

      try {
        const refreshed = await $fetch<{ accessToken: string }>(
          "/api/v1/auth/refresh",
          {
            method: "POST",
            baseURL,
          },
        );
        auth.setAccessToken(refreshed.accessToken);
        (options as { _authRetry?: boolean })._authRetry = true;
        return $api(request, options);
      } catch {
        await auth.logout();
      }
    },
  });

  return {
    provide: { api: $api },
  };
});
