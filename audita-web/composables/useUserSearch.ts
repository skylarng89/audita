import type { UserSearchResult } from "~/types"
import { useApi } from "~/composables/useApi"

export function useUserSearch() {
  const api = useApi()
  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  const results = ref<UserSearchResult[]>([])
  const searching = ref(false)

  function searchUsers(query: string, limit = 20): Promise<UserSearchResult[]> {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }

    return new Promise((resolve) => {
      if (!query.trim()) {
        results.value = []
        searching.value = false
        resolve([])
        return
      }

      searching.value = true

      debounceTimer = setTimeout(async () => {
        try {
          const data = await api<UserSearchResult[]>(
            "/api/v1/users/search",
            { query: { query: query.trim(), limit } },
          )
          results.value = data
          resolve(data)
        } catch {
          results.value = []
          resolve([])
        } finally {
          searching.value = false
        }
      }, 300)
    })
  }

  return {
    results,
    searching,
    searchUsers,
  }
}
