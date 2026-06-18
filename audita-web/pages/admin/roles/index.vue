<template>
  <div class="space-y-6">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Identity and Governance
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Roles & Permissions</h1>
        <p class="text-sm text-muted mt-1">
          Manage built-in and custom roles, and the permissions they grant.
        </p>
      </div>
      <button
        v-if="auth.hasPermission('roles.manage')"
        type="button"
        class="btn-primary btn-sm shadow-lg shadow-primary/20"
        @click="openCreate"
      >
        + Create Role
      </button>
    </div>

    <div v-if="pending" class="text-muted text-sm">Loading roles…</div>

    <div v-else class="space-y-4">
      <div
        v-for="role in roles"
        :key="role.id"
        class="card p-5 shadow-card-hover"
      >
        <div class="flex flex-wrap items-start justify-between gap-3 mb-3">
          <div class="space-y-1">
            <div class="flex items-center gap-2">
              <h2 class="font-semibold text-base">{{ role.name }}</h2>
              <AppBadge v-if="role.isSystem" variant="default">System</AppBadge>
              <AppBadge v-else variant="info">Custom</AppBadge>
              <span
                v-if="role.assignedUserCount != null"
                class="text-xs text-muted"
              >
                {{ role.assignedUserCount }}
                {{ role.assignedUserCount === 1 ? "user" : "users" }}
              </span>
            </div>
            <p v-if="role.description" class="text-sm text-muted">
              {{ role.description }}
            </p>
          </div>
          <div
            v-if="auth.hasPermission('roles.manage') && !role.isSystem"
            class="flex items-center gap-2"
          >
            <button
              type="button"
              class="btn-ghost btn-sm"
              @click="openEdit(role)"
            >
              Edit
            </button>
            <button
              type="button"
              class="btn-ghost btn-sm text-danger hover:bg-danger/10"
              @click="openDelete(role)"
            >
              Delete
            </button>
          </div>
        </div>
        <div class="flex flex-wrap gap-1.5">
          <span
            v-for="perm in role.permissions"
            :key="perm.id ?? perm.code"
            class="inline-block px-2 py-0.5 text-xs font-mono bg-muted/10 rounded"
            :title="perm.label"
          >
            {{ perm.code }}
          </span>
          <span v-if="role.permissions.length === 0" class="text-xs text-muted"
            >No permissions assigned.</span
          >
        </div>
      </div>
    </div>

    <p v-if="loadError" class="text-sm text-danger" role="alert">
      {{ loadError }}
    </p>

    <SharedAppModal
      v-model:open="showFormModal"
      :title="editingRole ? 'Edit Role' : 'Create Role'"
      size="lg"
    >
      <form @submit.prevent="saveRole" class="space-y-5">
        <div>
          <label class="field-label" for="role-name">
            Name <span class="text-danger">*</span>
          </label>
          <input
            id="role-name"
            v-model="form.name"
            type="text"
            class="input mt-1"
            placeholder="e.g., Release Manager"
            maxlength="100"
            required
          />
        </div>

        <div>
          <label class="field-label" for="role-description">
            Description
          </label>
          <textarea
            id="role-description"
            v-model="form.description"
            class="input mt-1"
            rows="2"
            placeholder="Optional short summary of this role's purpose"
            maxlength="500"
          />
        </div>

        <div>
          <p class="field-label mb-2">Permissions</p>
          <div class="space-y-4">
            <div
              v-for="group in permissionGroups"
              :key="group.label"
              class="rounded-lg border border-outline-variant/50 dark:border-border-dark p-3"
            >
              <div class="flex items-center justify-between mb-2">
                <p class="text-xs font-semibold uppercase tracking-wide text-muted">
                  {{ group.label }}
                </p>
                <button
                  type="button"
                  class="text-xs text-primary hover:underline"
                  @click="toggleGroup(group)"
                >
                  {{ isGroupAllSelected(group) ? "Clear" : "Select all" }}
                </button>
              </div>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <label
                  v-for="perm in group.permissions"
                  :key="perm.code"
                  class="flex items-start gap-2 text-sm cursor-pointer"
                >
                  <input
                    type="checkbox"
                    class="mt-0.5 h-4 w-4 accent-primary"
                    :checked="form.permissionCodes.includes(perm.code)"
                    @change="togglePermission(perm.code)"
                  />
                  <span>
                    <span class="font-mono text-xs">{{ perm.code }}</span>
                    <span
                      v-if="perm.label"
                      class="block text-xs text-muted"
                      >{{ perm.label }}</span
                    >
                  </span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <div
          v-if="formError"
          class="rounded-md bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
          role="alert"
        >
          {{ formError }}
        </div>
      </form>

      <template #footer>
        <button
          type="button"
          class="btn-ghost btn-sm"
          @click="closeForm"
        >
          Cancel
        </button>
        <button
          type="button"
          class="btn-primary btn-sm"
          :disabled="saving || !isFormValid"
          @click="saveRole"
        >
          {{ saving ? "Saving…" : editingRole ? "Save Changes" : "Create Role" }}
        </button>
      </template>
    </SharedAppModal>

    <SharedAppModal
      v-model:open="showDeleteModal"
      title="Delete Role"
      size="sm"
    >
      <div class="space-y-3 text-sm">
        <p>
          Are you sure you want to delete
          <strong>{{ deletingRole?.name }}</strong>?
        </p>
        <p v-if="deletingRole?.assignedUserCount" class="text-warning">
          {{ deletingRole.assignedUserCount }}
          {{ deletingRole.assignedUserCount === 1 ? "user is" : "users are" }}
          currently assigned to this role. They will need to be reassigned.
        </p>
        <p v-else class="text-muted">
          This role has no assigned users and can be safely removed.
        </p>
      </div>
      <template #footer>
        <button
          type="button"
          class="btn-ghost btn-sm"
          @click="closeDelete"
        >
          Cancel
        </button>
        <button
          type="button"
          class="btn-primary btn-sm bg-danger hover:bg-danger-dark"
          :disabled="deleting"
          @click="confirmDelete"
        >
          {{ deleting ? "Deleting…" : "Delete" }}
        </button>
      </template>
    </SharedAppModal>
  </div>
</template>

<script setup lang="ts">
import type { Permission, Role } from "~/composables/useRoles";
import { useRoles } from "~/composables/useRoles";
import { useLoadingOverlay } from "~/composables/useLoadingOverlay";

definePageMeta({ middleware: ["auth", "admin-only"] });

useHead({ title: "Roles & Permissions — Audita" });

const auth = useAuthStore();
const { success: toastSuccess, error: toastError } = useToast();
const { hide: hideLoading } = useLoadingOverlay();
const { listRoles, listPermissions, createRole, updateRole, deleteRole } =
  useRoles();

interface PermissionGroup {
  label: string;
  codes: string[];
  permissions: Permission[];
}

const PERMISSION_GROUPING: { label: string; codes: string[] }[] = [
  {
    label: "Change Requests",
    codes: [
      "cr.create",
      "cr.view",
      "cr.view.all",
      "cr.edit",
      "cr.cancel",
      "cr.submit",
      "cr.approve",
      "cr.manage_participants",
      "uat.signoff",
      "deployment.execute",
    ],
  },
  {
    label: "Users",
    codes: ["users.view", "users.manage"],
  },
  {
    label: "Roles",
    codes: ["roles.view", "roles.manage"],
  },
  {
    label: "Groups",
    codes: ["groups.view", "groups.manage"],
  },
  {
    label: "Settings",
    codes: ["settings.view", "settings.manage"],
  },
  {
    label: "SLA",
    codes: ["sla.view", "sla.manage"],
  },
  {
    label: "Audit",
    codes: ["audit.view", "audit.export"],
  },
];

const roles = ref<Role[]>([]);
const allPermissions = ref<Permission[]>([]);
const pending = ref(true);
const loadError = ref("");

const showFormModal = ref(false);
const showDeleteModal = ref(false);
const editingRole = ref<Role | null>(null);
const deletingRole = ref<Role | null>(null);
const saving = ref(false);
const deleting = ref(false);
const formError = ref("");

const form = reactive({
  name: "",
  description: "",
  permissionCodes: [] as string[],
});

const { data, pending: dataPending, refresh } = await useAsyncData<Role[]>(
  "roles-list",
  () => listRoles(),
);

const { data: permsData } = await useAsyncData<Permission[]>(
  "roles-permissions",
  () => listPermissions(),
);

watch(
  data,
  (val) => {
    roles.value = val ?? [];
  },
  { immediate: true },
);

watch(
  permsData,
  (val) => {
    allPermissions.value = val ?? [];
  },
  { immediate: true },
);

watch(dataPending, (val) => {
  pending.value = val;
  if (!val) hideLoading();
});

const permissionGroups = computed<PermissionGroup[]>(() => {
  const byCode = new Map(allPermissions.value.map((p) => [p.code, p]));
  return PERMISSION_GROUPING.map((g) => ({
    label: g.label,
    codes: g.codes,
    permissions: g.codes
      .map((code) => byCode.get(code))
      .filter((p): p is Permission => Boolean(p)),
  })).filter((g) => g.permissions.length > 0);
});

const selectedSortedKey = computed(() =>
  [...form.permissionCodes].sort().join(","),
);

const allPermissionCodes = computed(() =>
  allPermissions.value.map((p) => p.code).sort().join(","),
);

const existingRoleKeys = computed(() =>
  roles.value
    .filter((r) => r.id !== editingRole.value?.id)
    .map((r) => [...r.permissions.map((p) => p.code)].sort().join(",")),
);

const isFormValid = computed(() => {
  if (!form.name.trim()) return false;
  if (form.permissionCodes.length === 0) return false;
  if (selectedSortedKey.value === allPermissionCodes.value) return false;
  if (existingRoleKeys.value.includes(selectedSortedKey.value)) return false;
  return true;
});

function isGroupAllSelected(group: PermissionGroup): boolean {
  return group.permissions.every((p) =>
    form.permissionCodes.includes(p.code),
  );
}

function toggleGroup(group: PermissionGroup) {
  const allSelected = isGroupAllSelected(group);
  if (allSelected) {
    form.permissionCodes = form.permissionCodes.filter(
      (code) => !group.permissions.some((p) => p.code === code),
    );
  } else {
    const next = new Set(form.permissionCodes);
    group.permissions.forEach((p) => next.add(p.code));
    form.permissionCodes = Array.from(next);
  }
}

function togglePermission(code: string) {
  const idx = form.permissionCodes.indexOf(code);
  if (idx >= 0) {
    form.permissionCodes.splice(idx, 1);
  } else {
    form.permissionCodes.push(code);
  }
}

function openCreate() {
  editingRole.value = null;
  form.name = "";
  form.description = "";
  form.permissionCodes = [];
  formError.value = "";
  showFormModal.value = true;
}

function openEdit(role: Role) {
  editingRole.value = role;
  form.name = role.name;
  form.description = role.description ?? "";
  form.permissionCodes = role.permissions.map((p) => p.code);
  formError.value = "";
  showFormModal.value = true;
}

function closeForm() {
  showFormModal.value = false;
  editingRole.value = null;
  formError.value = "";
}

function validateForm(): string | null {
  if (!form.name.trim()) return "Name is required.";
  if (form.permissionCodes.length === 0)
    return "Select at least one permission.";
  if (selectedSortedKey.value === allPermissionCodes.value)
    return "A role cannot include every permission — use an existing admin role instead.";
  if (existingRoleKeys.value.includes(selectedSortedKey.value))
    return "Another role already grants this exact set of permissions.";
  return null;
}

async function saveRole() {
  formError.value = "";
  const validationError = validateForm();
  if (validationError) {
    formError.value = validationError;
    return;
  }

  saving.value = true;
  try {
    const body = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      permissionCodes: [...form.permissionCodes],
    };
    if (editingRole.value) {
      await updateRole(editingRole.value.id, body);
      toastSuccess("Role updated.");
    } else {
      await createRole(body);
      toastSuccess("Role created.");
    }
    closeForm();
    await refresh();
  } catch (error: unknown) {
    formError.value = resolveApiErrorMessage(
      error,
      "Failed to save role. Please try again.",
    );
  } finally {
    saving.value = false;
  }
}

function openDelete(role: Role) {
  deletingRole.value = role;
  showDeleteModal.value = true;
}

function closeDelete() {
  showDeleteModal.value = false;
  deletingRole.value = null;
}

async function confirmDelete() {
  if (!deletingRole.value) return;
  deleting.value = true;
  try {
    await deleteRole(deletingRole.value.id);
    toastSuccess("Role deleted.");
    closeDelete();
    await refresh();
  } catch (error: unknown) {
    toastError(
      resolveApiErrorMessage(error, "Failed to delete role. Please try again."),
    );
    closeDelete();
  } finally {
    deleting.value = false;
  }
}

onMounted(() => {
  if (roles.value.length === 0 && !pending.value) hideLoading();
});
</script>
