import type {
  Attachment,
  ApproverCandidate,
  ChangeRequest,
  ChangeRequestCustomFieldValue,
  CRStatus,
  CrApprover,
  Comment,
  ActivityEntry,
  Department,
  Deployment,
  DeploymentApprover,
  Group,
  Page,
  Uat,
  UatApprover,
} from "~/types";

export type ChangeRequestSearchResult = {
  id: string;
  displayId: string;
  title: string;
  status: CRStatus;
};
import { useApi } from "~/composables/useApi";

export function useChangeRequests() {
  const api = useApi();

  async function listCategories(): Promise<string[]> {
    return api<string[]>("/api/v1/change-requests/categories");
  }

  async function listActiveDepartments(): Promise<Department[]> {
    return api<Department[]>("/api/v1/settings/departments/active");
  }

  async function listActiveGroups(): Promise<Group[]> {
    return api<Group[]>("/api/v1/groups?active=true");
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

  async function updateApproverRequirement(
    id: string,
    approverId: string,
    isRequired: boolean,
  ): Promise<CrApprover> {
    return api<CrApprover>(
      `/api/v1/change-requests/${id}/approvers/${approverId}/requirement`,
      {
        method: "PATCH",
        query: { isRequired },
      },
    );
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

  async function searchRequests(
    query: string,
    limit = 10,
  ): Promise<ChangeRequestSearchResult[]> {
    return api<ChangeRequestSearchResult[]>(
      "/api/v1/change-requests/search",
      { query: { query, limit } },
    );
  }

  async function getLinkedRequests(id: string): Promise<string[]> {
    return api<string[]>(`/api/v1/change-requests/${id}/links`);
  }

  async function upsertLinks(
    id: string,
    linkedRequestIds: string[],
  ): Promise<void> {
    await api(`/api/v1/change-requests/${id}/links`, {
      method: "PUT",
      body: { linkedRequestIds },
    });
  }

  async function markComplete(requestId: string): Promise<ChangeRequest> {
    return api<ChangeRequest>(
      `/api/v1/change-requests/${requestId}/complete`,
      { method: "POST" },
    );
  }

  async function getDeployment(requestId: string): Promise<Deployment> {
    return api<Deployment>(
      `/api/v1/change-requests/${requestId}/deployment`,
    );
  }

  async function approveDeployment(requestId: string): Promise<void> {
    await api(`/api/v1/change-requests/${requestId}/deployment/approve`, {
      method: "POST",
    });
  }

  async function rejectDeployment(
    requestId: string,
    reason: string,
  ): Promise<void> {
    await api(`/api/v1/change-requests/${requestId}/deployment/reject`, {
      method: "POST",
      body: { reason },
    });
  }

  async function listDeploymentApprovers(
    requestId: string,
  ): Promise<DeploymentApprover[]> {
    return api<DeploymentApprover[]>(
      `/api/v1/change-requests/${requestId}/deployment/approvers`,
    );
  }

  async function getUat(requestId: string): Promise<Uat> {
    return api<Uat>(`/api/v1/change-requests/${requestId}/uat`);
  }

  async function createUat(
    requestId: string,
    body: { title: string; details: string },
  ): Promise<Uat> {
    return api<Uat>(`/api/v1/change-requests/${requestId}/uat`, {
      method: "POST",
      body,
    });
  }

  async function updateUat(
    requestId: string,
    body: { title: string; details: string },
  ): Promise<Uat> {
    return api<Uat>(`/api/v1/change-requests/${requestId}/uat`, {
      method: "PATCH",
      body,
    });
  }

  async function addUatApprover(
    requestId: string,
    body: { userId: string; isRequired: boolean },
  ): Promise<void> {
    await api(`/api/v1/change-requests/${requestId}/uat/approvers`, {
      method: "POST",
      body,
    });
  }

  async function listUatApprovers(requestId: string): Promise<UatApprover[]> {
    return api<UatApprover[]>(`/api/v1/change-requests/${requestId}/uat/approvers`);
  }

  async function approveUat(requestId: string): Promise<void> {
    await api(`/api/v1/change-requests/${requestId}/uat/approve`, {
      method: "POST",
    });
  }

  async function rejectUat(requestId: string, reason: string): Promise<void> {
    await api(`/api/v1/change-requests/${requestId}/uat/reject`, {
      method: "POST",
      body: { reason },
    });
  }

  async function promoteUat(requestId: string): Promise<void> {
    await api(`/api/v1/change-requests/${requestId}/uat/promote`, {
      method: "POST",
    });
  }

  async function listUatComments(requestId: string): Promise<Comment[]> {
    return api<Comment[]>(`/api/v1/change-requests/${requestId}/uat/comments`);
  }

  async function postUatComment(requestId: string, body: string): Promise<Comment> {
    return api<Comment>(`/api/v1/change-requests/${requestId}/uat/comments`, {
      method: "POST",
      body: { body },
    });
  }

  async function listDeploymentComments(requestId: string): Promise<Comment[]> {
    return api<Comment[]>(`/api/v1/change-requests/${requestId}/deployment/comments`);
  }

  async function postDeploymentComment(requestId: string, body: string): Promise<Comment> {
    return api<Comment>(`/api/v1/change-requests/${requestId}/deployment/comments`, {
      method: "POST",
      body: { body },
    });
  }

  return {
    listCategories,
    listActiveDepartments,
    listActiveGroups,
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
    updateApproverRequirement,
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
    searchRequests,
    getLinkedRequests,
    upsertLinks,
    getUat,
    createUat,
    updateUat,
    addUatApprover,
    listUatApprovers,
    approveUat,
    rejectUat,
    promoteUat,
    markComplete,
    getDeployment,
    approveDeployment,
    rejectDeployment,
    listDeploymentApprovers,
    listUatComments,
    postUatComment,
    listDeploymentComments,
    postDeploymentComment,
  };
}
