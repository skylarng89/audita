<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Identity and Access
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Users</h1>
        <p class="text-sm text-muted mt-1">
          View all users in your organisation and their assigned roles.
        </p>
      </div>
      <button
        v-if="canManageUsers"
        @click="showInviteModal = true"
        class="btn-primary btn-sm"
      >
        + Invite User
      </button>
    </div>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
      <section class="card p-5 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">
          Total Users
        </p>
        <p class="mt-2 text-3xl font-bold">{{ totalUsers }}</p>
      </section>
      <section class="card p-5 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">
          Active
        </p>
        <p class="mt-2 text-3xl font-bold text-success">{{ activeUsers }}</p>
      </section>
      <section class="card p-5 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">
          Suspended
        </p>
        <p class="mt-2 text-3xl font-bold text-danger">{{ suspendedUsers }}</p>
      </section>
    </div>

    <section class="card shadow-card-hover">
      <SharedAppTable
        :columns="columns"
        :data="users as unknown as Record<string, unknown>[]"
        :loading="pending"
        row-key="id"
        empty-message="No users found for this organisation."
      >
        <template #cell-roleName="{ value }">
          <SharedAppBadge variant="primary">{{
            value || "Unassigned"
          }}</SharedAppBadge>
        </template>

        <template #cell-status="{ value }">
          <SharedAppBadge :variant="statusVariant(value)">{{
            value || "UNKNOWN"
          }}</SharedAppBadge>
        </template>
        <template v-if="canManageUsers" #cell-actions="{ row }">
          <div class="flex gap-2">
            <template v-if="row.status === 'PENDING'">
              <button
                @click="resendInvite(row.id as string)"
                class="text-xs text-primary hover:underline"
              >
                Resend
              </button>
              <button
                @click="cancelInvite(row.id as string)"
                class="text-xs text-danger hover:underline"
              >
                Cancel
              </button>
            </template>
            <template v-else>
              <button
                v-if="row.status !== 'SUSPENDED' && row.id !== auth.userId"
                @click="deactivate(row.id as string)"
                class="text-xs text-danger hover:underline"
              >
                Deactivate
              </button>
              <button
                v-if="row.status === 'SUSPENDED'"
                @click="reactivate(row.id as string)"
                class="text-xs text-success hover:underline"
              >
                Reactivate
              </button>
            </template>
          </div>
        </template>
      </SharedAppTable>

      <div
        v-if="total > pageSize"
        class="border-t border-border px-5 py-4 dark:border-border-dark"
      >
        <SharedAppPagination
          :page="page"
          :page-size="pageSize"
          :total="total"
          @update:page="onPageChange"
        />
      </div>
    </section>

    <SharedAppModal
      v-if="showInviteModal"
      :open="showInviteModal"
      title="Invite User"
      @close="showInviteModal = false"
    >
      <form @submit.prevent="inviteUser" class="space-y-4 p-4">
        <div
          v-if="inviteError"
          class="rounded-md border border-danger-border bg-danger-light px-4 py-3 text-sm text-danger"
        >
          {{ inviteError }}
        </div>

        <div>
          <label
            for="invite-email"
            class="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-muted"
            >Email</label
          >
          <input
            id="invite-email"
            v-model="inviteForm.email"
            type="email"
            class="input"
            required
          />
        </div>

        <div>
          <label
            for="invite-full-name"
            class="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-muted"
            >Full Name</label
          >
          <input
            id="invite-full-name"
            v-model="inviteForm.fullName"
            type="text"
            class="input"
            required
          />
        </div>

        <div>
          <label
            for="invite-role"
            class="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-muted"
            >Role</label
          >
          <select
            id="invite-role"
            v-model="inviteForm.roleId"
            class="input"
            required
          >
            <option value="" disabled>Select a role</option>
            <option v-for="role in roles" :key="role.id" :value="role.id">
              {{ role.name }}
            </option>
          </select>
        </div>

        <div class="flex justify-end gap-2 pt-2">
          <button
            type="button"
            @click="showInviteModal = false"
            class="btn-ghost btn-sm"
          >
            Cancel
          </button>
          <button type="submit" class="btn-primary btn-sm" :disabled="inviting">
            {{ inviting ? "Sending..." : "Send Invite" }}
          </button>
        </div>
      </form>
    </SharedAppModal>

    <p v-if="loadError" class="text-sm text-danger">{{ loadError }}</p>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: "default",
  middleware: ["role"],
  requiredRole: "Admin",
});

useHead({ title: "Users — Audita" });

const api = useApi();
const auth = useAuthStore();
const { success: toastSuccess, error: toastError } = useToast();

interface UserRow {
  id: string;
  fullName: string;
  email: string;
  roleName?: string;
  status?: string;
}

interface UserPayload {
  content?: UserRow[];
  totalElements?: number;
  page?: {
    totalElements?: number;
  };
}

interface Role {
  id: string;
  name: string;
}

const page = ref(1);
const pageSize = 20;
const loadError = ref("");
const showInviteModal = ref(false);
const inviteError = ref("");
const inviting = ref(false);
const inviteForm = reactive({
  email: "",
  fullName: "",
  roleId: "",
});

const { data, pending, refresh } = await useAsyncData<UserPayload>(
  "users-index-page",
  async () => {
    loadError.value = "";

    if (!auth.tenantSlug) {
      loadError.value =
        "Tenant context is missing. Sign out and sign in to your organisation again.";
      return { content: [], totalElements: 0 };
    }

    try {
      return (await api(
        `/api/v1/users?page=${page.value - 1}&size=${pageSize}`,
      )) as UserPayload;
    } catch (err: unknown) {
      const apiErr = err as { status?: number; data?: { message?: string } };
      if (apiErr.status === 403) {
        loadError.value = "You do not have permission to view users.";
      } else if (apiErr.status === 401) {
        loadError.value = "Your session expired. Please sign in again.";
      } else {
        loadError.value = apiErr.data?.message ?? "Failed to load users.";
      }
      return { content: [], totalElements: 0 };
    }
  },
  { watch: [page] },
);

const { data: rolesData } = await useAsyncData<Role[]>(
  "users-roles",
  async () => {
    try {
      return (await api("/api/v1/roles")) as Role[];
    } catch {
      return [];
    }
  },
);

const users = computed(() => data.value?.content ?? []);
const total = computed(
  () =>
    data.value?.totalElements ??
    data.value?.page?.totalElements ??
    users.value.length,
);
const roles = computed(() => rolesData.value ?? []);
const canManageUsers = computed(
  () => auth.role === "Admin" || auth.role === "SUPER_ADMIN",
);

const totalUsers = computed(
  () => data.value?.totalElements ?? users.value.length,
);
const activeUsers = computed(
  () => users.value.filter((user) => user.status === "ACTIVE").length,
);
const suspendedUsers = computed(
  () => users.value.filter((user) => user.status === "SUSPENDED").length,
);

function statusVariant(status: unknown): "success" | "danger" | "neutral" {
  if (status === "ACTIVE") {
    return "success";
  }

  if (status === "SUSPENDED") {
    return "danger";
  }

  return "neutral";
}

function onPageChange(nextPage: number) {
  page.value = nextPage;
  refresh();
}

async function inviteUser() {
  inviteError.value = "";
  inviting.value = true;

  try {
    await api("/api/v1/users/invite", {
      method: "POST",
      body: {
        email: inviteForm.email,
        fullName: inviteForm.fullName,
        roleId: inviteForm.roleId,
      },
    });

    toastSuccess("Invite sent.");
    showInviteModal.value = false;
    inviteForm.email = "";
    inviteForm.fullName = "";
    inviteForm.roleId = "";
    refresh();
  } catch (err: unknown) {
    const apiErr = err as { data?: { message?: string } };
    inviteError.value = apiErr.data?.message ?? "Failed to send invite.";
  } finally {
    inviting.value = false;
  }
}

const columns = computed(() => {
  const base = [
    { key: "fullName", label: "Name" },
    { key: "email", label: "Email" },
    { key: "roleName", label: "Role" },
    { key: "status", label: "Status" },
  ];
  if (canManageUsers.value) {
    base.push({ key: "actions", label: "" });
  }
  return base;
});

async function deactivate(id: string) {
  try {
    await api(`/api/v1/users/${id}/deactivate`, { method: "POST" });
    toastSuccess("User deactivated.");
    refresh();
  } catch {
    toastError("Failed to deactivate user.");
  }
}

async function reactivate(id: string) {
  try {
    await api(`/api/v1/users/${id}/reactivate`, { method: "POST" });
    toastSuccess("User reactivated.");
    refresh();
  } catch {
    toastError("Failed to reactivate user.");
  }
}

async function resendInvite(id: string) {
  try {
    await api(`/api/v1/users/${id}/invite`, { method: "POST" });
    toastSuccess("Invite resent.");
    refresh();
  } catch {
    toastError("Failed to resend invite.");
  }
}

async function cancelInvite(id: string) {
  try {
    await api(`/api/v1/users/${id}/invite`, { method: "DELETE" });
    toastSuccess("Invite cancelled.");
    refresh();
  } catch {
    toastError("Failed to cancel invite.");
  }
}
</script>
