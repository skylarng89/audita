import type {
  Attachment,
  ApproverCandidate,
  ChangeRequest,
  ChangeRequestCustomFieldValue,
  CrApprover,
  Comment,
  ActivityEntry,
  Page,
} from "~/types";

export function useChangeRequests() {
  const api = useApi();

  async function listCategories(): Promise<string[]> {
    return api<string[]>("/api/v1/change-requests/categories");
  }

  async function list(params?: {
    status?: string;
    priority?: string;
    category?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<Page<ChangeRequest>> {
    return api<Page<ChangeRequest>>("/api/v1/change-requests", {
      query: params,
    });
  }

  async function get(id: string): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}`);
  }

  async function create(body: Record<string, unknown>): Promise<ChangeRequest> {
    return api<ChangeRequest>("/api/v1/change-requests", {
      method: "POST",
      body,
    });
  }

  async function update(
    id: string,
    body: Record<string, unknown>,
  ): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}`, {
      method: "PATCH",
      body,
    });
  }

  async function submit(id: string): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}/submit`, {
      method: "POST",
    });
  }

  async function cancel(id: string): Promise<void> {
    await api(`/api/v1/change-requests/${id}/cancel`, { method: "POST" });
  }

  async function approve(id: string): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}/approve`, {
      method: "POST",
    });
  }

  async function reject(id: string, reason: string): Promise<ChangeRequest> {
    return api<ChangeRequest>(`/api/v1/change-requests/${id}/reject`, {
      method: "POST",
      body: { reason },
    });
  }

  async function listApprovers(id: string): Promise<CrApprover[]> {
    return api<CrApprover[]>(`/api/v1/change-requests/${id}/approvers`);
  }

  async function addApprover(
    id: string,
    body: { userId: string; isRequired: boolean },
  ): Promise<CrApprover> {
    return api<CrApprover>(`/api/v1/change-requests/${id}/approvers`, {
      method: "POST",
      body,
    });
  }

  async function addApproverGroup(
    id: string,
    body: { groupId: string; isRequired: boolean },
  ): Promise<CrApprover[]> {
    return api<CrApprover[]>(`/api/v1/change-requests/${id}/approvers/groups`, {
      method: "POST",
      body,
    });
  }

  async function searchApproverCandidates(
    query: string,
    limit = 10,
  ): Promise<ApproverCandidate[]> {
    return api<ApproverCandidate[]>(
      `/api/v1/change-requests/approver-candidates`,
      {
        query: {
          query,
          limit,
        },
      },
    );
  }

  async function reorderApprovers(
    id: string,
    approverIds: string[],
  ): Promise<CrApprover[]> {
    return api<CrApprover[]>(
      `/api/v1/change-requests/${id}/approvers/reorder`,
      {
        method: "PATCH",
        body: { approverIds },
      },
    );
  }

  async function removeApprover(id: string, approverId: string): Promise<void> {
    await api(`/api/v1/change-requests/${id}/approvers/${approverId}`, {
      method: "DELETE",
    });
  }

  async function listCustomFields(
    id: string,
  ): Promise<ChangeRequestCustomFieldValue[]> {
    return api<ChangeRequestCustomFieldValue[]>(
      `/api/v1/change-requests/${id}/custom-fields`,
    );
  }

  async function saveCustomFields(
    id: string,
    fields: ChangeRequestCustomFieldValue[],
  ): Promise<ChangeRequestCustomFieldValue[]> {
    return api<ChangeRequestCustomFieldValue[]>(
      `/api/v1/change-requests/${id}/custom-fields`,
      {
        method: "PUT",
        body: { fields },
      },
    );
  }

  async function listAttachments(id: string): Promise<Attachment[]> {
    return api<Attachment[]>(`/api/v1/change-requests/${id}/attachments`);
  }

  async function uploadAttachment(id: string, file: File): Promise<Attachment> {
    const formData = new FormData();
    formData.append("file", file);
    return api<Attachment>(`/api/v1/change-requests/${id}/attachments`, {
      method: "POST",
      body: formData,
    });
  }

  async function downloadAttachment(
    crId: string,
    attachmentId: string,
    fileName: string,
  ): Promise<void> {
    const auth = useAuthStore();
    const config = useRuntimeConfig();
    // Always use the public base URL — this runs in the browser.
    const base = config.public.apiBase as string;
    const url = `${base}/api/v1/change-requests/${crId}/attachments/${attachmentId}/download`;

    const res = await fetch(url, {
      headers: {
        ...(auth.accessToken
          ? { Authorization: `Bearer ${auth.accessToken}` }
          : {}),
        ...(auth.tenantSlug ? { "X-Tenant-Slug": auth.tenantSlug } : {}),
      },
    });

    if (!res.ok) {
      throw new Error(`Download failed: ${res.status}`);
    }

    const blob = await res.blob();
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = objectUrl;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(objectUrl);
  }

  async function listComments(id: string): Promise<Comment[]> {
    return api<Comment[]>(`/api/v1/change-requests/${id}/comments`);
  }

  async function postComment(id: string, body: string): Promise<Comment> {
    return api<Comment>(`/api/v1/change-requests/${id}/comments`, {
      method: "POST",
      body: { body },
    });
  }

  async function listActivity(id: string): Promise<ActivityEntry[]> {
    return api<ActivityEntry[]>(`/api/v1/change-requests/${id}/activity`);
  }

  return {
    listCategories,
    list,
    get,
    create,
    update,
    submit,
    cancel,
    approve,
    reject,
    listApprovers,
    addApprover,
    addApproverGroup,
    removeApprover,
    reorderApprovers,
    searchApproverCandidates,
    listCustomFields,
    saveCustomFields,
    listAttachments,
    uploadAttachment,
    downloadAttachment,
    listComments,
    postComment,
    listActivity,
  };
}
