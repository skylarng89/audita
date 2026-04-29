<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Identity and Access
        </p>
        <h1 class="text-3xl font-bold tracking-tight">User Management</h1>
        <p class="text-sm text-muted mt-1">
          Manage users in your organisation.
        </p>
      </div>
      <button
        @click="showInviteModal = true"
        class="btn-primary btn-sm shadow-lg shadow-primary/20"
      >
        + Invite User
      </button>
    </div>

    <div class="card shadow-card-hover">
      <AppTable :columns="columns" :rows="users" :loading="pending">
        <template #cell-roleName="{ row }">
          <AppBadge variant="default">{{ row.roleName ?? "—" }}</AppBadge>
        </template>
        <template #cell-status="{ row }">
          <AppBadge :variant="statusVariant(row.status)">{{
            row.status
          }}</AppBadge>
        </template>
        <template #cell-actions="{ row }">
          <div class="flex gap-2">
            <button
              @click="openEdit(row)"
              class="text-xs text-primary hover:underline"
            >
              Edit
            </button>
            <button
              v-if="row.status !== 'SUSPENDED'"
              @click="deactivate(row.id)"
              class="text-xs text-danger hover:underline"
            >
              Deactivate
            </button>
            <button
              v-else
              @click="reactivate(row.id)"
              class="text-xs text-success hover:underline"
            >
              Reactivate
            </button>
          </div>
        </template>
      </AppTable>
      <div class="px-5 py-4 border-t border-border dark:border-border-dark">
        <AppPagination
          :current-page="page"
          :total-pages="totalPages"
          @change="
            (p) => {
              page = p;
              refresh();
            }
          "
        />
      </div>
    </div>

    <!-- Invite Modal -->
    <AppModal
      v-if="showInviteModal"
      title="Invite User"
      @close="showInviteModal = false"
    >
      <form @submit.prevent="inviteUser" class="space-y-4 p-4">
        <div
          v-if="inviteError"
          class="rounded-md bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
        >
          {{ inviteError }}
        </div>
        <div>
          <label
            class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5"
            >Email</label
          >
          <input
            v-model="inviteForm.email"
            type="email"
            class="input"
            required
          />
        </div>
        <div>
          <label
            class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5"
            >Full Name</label
          >
          <input
            v-model="inviteForm.fullName"
            type="text"
            class="input"
            required
          />
        </div>
        <div>
          <label
            class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5"
            >Role</label
          >
          <select v-model="inviteForm.roleId" class="input">
            <option v-for="r in roles" :key="r.id" :value="r.id">
              {{ r.name }}
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
            {{ inviting ? "Sending…" : "Send Invite" }}
          </button>
        </div>
      </form>
    </AppModal>

    <!-- Edit Role Modal -->
    <AppModal
      v-if="editUser"
      :title="`Edit ${editUser.fullName}`"
      @close="editUser = null"
    >
      <form @submit.prevent="updateUser" class="space-y-4 p-4">
        <div>
          <label
            class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5"
            >Role</label
          >
          <select v-model="editForm.roleId" class="input">
            <option v-for="r in roles" :key="r.id" :value="r.id">
              {{ r.name }}
            </option>
          </select>
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <button
            type="button"
            @click="editUser = null"
            class="btn-ghost btn-sm"
          >
            Cancel
          </button>
          <button type="submit" class="btn-primary btn-sm">Save</button>
        </div>
      </form>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "default" });

const api = useApi();
const { addToast } = useToast();

interface UserRow {
  id: string;
  email: string;
  fullName: string;
  roleId: string;
  roleName: string;
  status: string;
}
interface Role {
  id: string;
  name: string;
}
interface PageData {
  content: UserRow[];
  totalPages: number;
  number: number;
}

const page = ref(0);
const { data, pending, refresh } = await useAsyncData<PageData>(
  "admin-users",
  () => api(`/api/v1/users?page=${page.value}&size=20`) as Promise<PageData>,
);
const { data: rolesData } = await useAsyncData<Role[]>(
  "roles",
  () => api("/api/v1/roles") as Promise<Role[]>,
);

const users = computed(() => data.value?.content ?? []);
const totalPages = computed(() => data.value?.totalPages ?? 1);
const roles = computed(() => rolesData.value ?? []);

const showInviteModal = ref(false);
const inviteForm = reactive({ email: "", fullName: "", roleId: "" });
const inviteError = ref("");
const inviting = ref(false);

const editUser = ref<UserRow | null>(null);
const editForm = reactive({ roleId: "" });

function openEdit(user: UserRow) {
  editUser.value = user;
  editForm.roleId = user.roleId;
}

function statusVariant(status: string) {
  if (status === "ACTIVE") return "success";
  if (status === "SUSPENDED") return "danger";
  return "default";
}

async function inviteUser() {
  inviteError.value = "";
  inviting.value = true;
  try {
    await api("/api/v1/users/invite", { method: "POST", body: inviteForm });
    addToast({ type: "success", message: "Invite sent!" });
    showInviteModal.value = false;
    refresh();
  } catch (e: unknown) {
    const err = e as { data?: { message?: string } };
    inviteError.value = err.data?.message ?? "Failed to send invite.";
  } finally {
    inviting.value = false;
  }
}

async function updateUser() {
  if (!editUser.value) return;
  try {
    await api(`/api/v1/users/${editUser.value.id}`, {
      method: "PATCH",
      body: { roleId: editForm.roleId },
    });
    addToast({ type: "success", message: "User updated." });
    editUser.value = null;
    refresh();
  } catch {
    addToast({ type: "error", message: "Failed to update user." });
  }
}

async function deactivate(id: string) {
  await api(`/api/v1/users/${id}/deactivate`, { method: "POST" });
  addToast({ type: "success", message: "User deactivated." });
  refresh();
}

async function reactivate(id: string) {
  await api(`/api/v1/users/${id}/reactivate`, { method: "POST" });
  addToast({ type: "success", message: "User reactivated." });
  refresh();
}

const columns = [
  { key: "fullName", label: "Name" },
  { key: "email", label: "Email" },
  { key: "roleName", label: "Role" },
  { key: "status", label: "Status" },
  { key: "actions", label: "" },
];
</script>
