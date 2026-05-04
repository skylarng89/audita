/**
 * Typed wrapper around the $api plugin.
 * All composables import from here — never call $fetch directly in components.
 */
type ApiRequestOptions = {
  method?: string;
  headers?: HeadersInit;
  query?: Record<string, unknown>;
  body?: unknown;
  credentials?: RequestCredentials;
};

export function useApi() {
  const { $api } = useNuxtApp();
  const caller = $api as unknown as <T = unknown>(
    request: string,
    options?: ApiRequestOptions,
  ) => Promise<T>;

  return function api<T = unknown>(
    request: string,
    options?: ApiRequestOptions,
  ): Promise<T> {
    return caller<T>(request, options);
  };
}
