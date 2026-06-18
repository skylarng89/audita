import { useApi } from "~/composables/useApi";

export interface Permission {
  id: string;
  code: string;
  label: string;
}

export interface Role {
  id: string;
  name: string;
  description: string;
  isSystem: boolean;
  permissions: Permission[];
  assignedUserCount?: number;
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
  permissionCodes: string[];
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
  permissionCodes?: string[];
}

export function useRoles() {
  const api = useApi();

  async function listRoles(): Promise<Role[]> {
    return api<Role[]>("/api/v1/roles");
  }

  async function listPermissions(): Promise<Permission[]> {
    const res = await api<{ permissions: Permission[] }>(
      "/api/v1/roles/permissions",
    );
    return res.permissions ?? [];
  }

  async function createRole(body: CreateRoleRequest): Promise<Role> {
    return api<Role>("/api/v1/roles", { method: "POST", body });
  }

  async function updateRole(id: string, body: UpdateRoleRequest): Promise<Role> {
    return api<Role>(`/api/v1/roles/${id}`, { method: "PATCH", body });
  }

  async function deleteRole(id: string): Promise<void> {
    await api(`/api/v1/roles/${id}`, { method: "DELETE" });
  }

  return {
    listRoles,
    listPermissions,
    createRole,
    updateRole,
    deleteRole,
  };
}
