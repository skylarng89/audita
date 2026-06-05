/**
 * Shared theme composable. Centralizes dark mode toggle logic and
 * localStorage persistence so layouts and components don't duplicate it.
 */
const COLOR_SCHEME_KEY = "color-scheme"

const isDark = ref(false)

function initTheme() {
  const stored = localStorage.getItem(COLOR_SCHEME_KEY)
  const prefersDark = globalThis.matchMedia(
    "(prefers-color-scheme: dark)",
  ).matches
  isDark.value = stored ? stored === "dark" : prefersDark
  applyTheme()
}

function applyTheme() {
  document.documentElement.classList.toggle("dark", isDark.value)
}

function toggle() {
  isDark.value = !isDark.value
  localStorage.setItem(COLOR_SCHEME_KEY, isDark.value ? "dark" : "light")
  applyTheme()
}

export function useTheme() {
  return { isDark, initTheme, toggle }
}
