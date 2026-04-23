/**
 * Typed wrapper around the $api plugin.
 * All composables import from here — never call $fetch directly in components.
 */
export function useApi() {
  const { $api } = useNuxtApp()
  return $api as typeof $fetch
}
