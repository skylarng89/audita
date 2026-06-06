import type { CreateGroupRequest, Group, Page, User } from "~/types"
import { useApi } from "~/composables/useApi"

export function useGroups() {
  const api = useApi()

  async function fetchGroups(page: number, size = 20): Promise<Page<Group>> {
    return api<Page<Group>>("/api/v1/groups", {
      query: { page: page - 1, size },
    })
  }

  async function fetchGroup(id: string): Promise<Group> {
    return api<Group>(`/api/v1/groups/${id}`)
  }

  async function createGroup(body: CreateGroupRequest): Promise<Group> {
    return api<Group>("/api/v1/groups", {
      method: "POST",
      body,
    })
  }

  async function deleteGroup(id: string): Promise<void> {
    await api(`/api/v1/groups/${id}`, { method: "DELETE" })
  }

  async function fetchGroupMembers(
    groupId: string,
    page: number,
    size = 20,
  ): Promise<Page<User>> {
    return api<Page<User>>(
      `/api/v1/groups/${groupId}/members?page=${page - 1}&size=${size}`,
    )
  }

  async function addGroupMembers(
    groupId: string,
    userIds: string[],
  ): Promise<void> {
    await api(`/api/v1/groups/${groupId}/members/batch`, {
      method: "POST",
      body: { userIds },
    })
  }

  async function removeGroupMembers(
    groupId: string,
    userIds: string[],
  ): Promise<void> {
    await api(`/api/v1/groups/${groupId}/members/batch`, {
      method: "DELETE",
      body: { userIds },
    })
  }

  return {
    fetchGroups,
    fetchGroup,
    createGroup,
    deleteGroup,
    fetchGroupMembers,
    addGroupMembers,
    removeGroupMembers,
  }
}
