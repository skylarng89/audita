/**
 * Runs synchronously on both server and client before the first render.
 * Hydrating the auth store here ensures SSR and client produce identical
 * HTML for auth-gated elements, preventing hydration mismatches.
 */
export default defineNuxtPlugin(() => {
  const auth = useAuthStore();
  auth.hydrateFromCookie();
});
