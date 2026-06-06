import type { Page, User } from "~/types"
import { useApi } from "~/composables/useApi"

export function useUsers() {
  const api = useApi()

  async function listUsers(page: number, size = 20): Promise<Page<User>> {
    return api<Page<User>>("/api/v1/users", {
      query: { page: page - 1, size },
    })
  }

  async function inviteUser(body: {
    email: string
    fullName: string
    roleId: string
    groupIds?: string[]
  }): Promise<void> {
    await api("/api/v1/users/invite", {
      method: "POST",
      body,
    })
  }

  return {
    listUsers,
    inviteUser,
  }
}
