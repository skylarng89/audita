import { shouldAttemptTokenRefresh } from "~/composables/authSession";
import { useAuthStore } from "~/stores/auth";
import type { AuthResponse } from "~/types";

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig();
  const auth = useAuthStore();
  const baseURL = import.meta.server
    ? config.apiInternalBase
    : config.public.apiBase;
  let refreshPromise: Promise<AuthResponse | null> | null = null;
  let apiClient: ReturnType<typeof $fetch.create>;
  type ApiClientOptions = Parameters<typeof apiClient>[1];
  interface OnResponseErrorContext {
    request: Request | string;
    options: ApiClientOptions & { _authRetry?: boolean };
    response: {
      status: number;
    };
  }

  function refreshSession() {
    if (!refreshPromise) {
      refreshPromise = $fetch<AuthResponse>("/api/v1/auth/refresh", {
        method: "POST",
        baseURL,
      })
        .then((refreshed) => {
          auth.setAuth(refreshed);
          return refreshed;
        })
        .catch(async (error) => {
          console.error("Token refresh failed", error);
          await auth.logout();
          return null;
        })
        .finally(() => {
          refreshPromise = null;
        });
    }

    return refreshPromise;
  }

  apiClient = $fetch.create({
    baseURL,

    onRequest({ request, options }) {
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
      if (auth.isAuthenticated && auth.accessToken) {
        headers.set("Authorization", `Bearer ${auth.accessToken}`);
      }

      // Inject tenant slug for routing
      if (auth.tenantSlug) {
        headers.set("X-Tenant-Slug", auth.tenantSlug);
      }

      options.headers = headers;
    },

    onResponseError: (async (ctx: OnResponseErrorContext) => {
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
      const shouldRefresh = shouldAttemptTokenRefresh({
        alreadyRetried,
        isAuthenticated: auth.isAuthenticated,
        isRefreshEndpoint,
        responseStatus: response.status,
      });

      if (!shouldRefresh) {
        return;
      }

      const refreshed = await refreshSession();
      if (!refreshed) {
        return;
      }

      (options as { _authRetry?: boolean })._authRetry = true;
      return apiClient(request, options as ApiClientOptions);
    }) as never,
  });

  return {
    provide: { api: apiClient },
  };
});
