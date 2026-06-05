export function isSafeRedirect(redirect: string | null | undefined): boolean {
  if (!redirect) return false
  if (!redirect.startsWith("/") || redirect.startsWith("//")) return false
  if (/^[a-zA-Z][a-zA-Z0-9+\-.]*:/.test(redirect)) return false
  return true
}
