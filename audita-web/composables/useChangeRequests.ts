import type { ChangeRequest, CrApprover, Comment, ActivityEntry, Page } from '~/types'

export function useChangeRequests() {
  const api = useApi()

  async function list(params?: {
    status?: string
    priority?: string
    category?: string
    page?: number
    size?: number
    sort?: string
  }): Promise<Page<ChangeRequest>> {
    return api<Page<ChangeRequest>>('/api/v1/change-requests', { query: params })
  }

  async function get(id: string): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}`)
  }

  async function create(body: Partial<ChangeRequest>): Promise<ChangeRequest> {
    return api<ChangeRequest>('/api/v1/change-requests', { method: 'POST', body })
  }

  async function update(id: string, body: Partial<ChangeRequest>): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}`, { method: 'PATCH', body })
  }

  async function submit(id: string): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}/submit`, { method: 'POST' })
  }

  async function cancel(id: string): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}/cancel`, { method: 'POST' })
  }

  async function approve(id: string): Promise<void> {
    return api(`/api/v1/change-requests/${id}/approve`, { method: 'POST' })
  }

  async function reject(id: string, reason: string): Promise<void> {
    return api(`/api/v1/change-requests/${id}/reject`, {
      method: 'POST',
      body: { reason },
    })
  }

  async function listApprovers(id: string): Promise<CrApprover[]> {
    return api<CrApprover[]>(`/api/v1/change-requests/${id}/approvers`)
  }

  async function addApprover(id: string, body: { userId: string; isRequired: boolean }): Promise<CrApprover> {
    return api<CrApprover>(`/api/v1/change-requests/${id}/approvers`, { method: 'POST', body })
  }

  async function removeApprover(id: string, approverId: string): Promise<void> {
    return api(`/api/v1/change-requests/${id}/approvers/${approverId}`, { method: 'DELETE' })
  }

  async function listComments(id: string): Promise<Comment[]> {
    return api<Comment[]>(`/api/v1/change-requests/${id}/comments`)
  }

  async function postComment(id: string, body: string): Promise<Comment> {
    return api<Comment>(`/api/v1/change-requests/${id}/comments`, {
      method: 'POST',
      body: { body },
    })
  }

  async function listActivity(id: string): Promise<ActivityEntry[]> {
    return api<ActivityEntry[]>(`/api/v1/change-requests/${id}/activity`)
  }

  return {
    list, get, create, update, submit, cancel,
    approve, reject,
    listApprovers, addApprover, removeApprover,
    listComments, postComment, listActivity,
  }
}
