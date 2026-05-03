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
      <AppTable
        :columns="columns"
        :data="users"
        :loading="pending"
        row-key="id"
        empty-message="No users found for this organisation."
      >
        <template #cell-roleName="{ value }">
          <AppBadge variant="primary">{{ value || "Unassigned" }}</AppBadge>
        </template>

        <template #cell-status="{ value }">
          <AppBadge :variant="statusVariant(value)">{{
            value || "UNKNOWN"
          }}</AppBadge>
        </template>
      </AppTable>

      <div
        v-if="total > pageSize"
        class="border-t border-border px-5 py-4 dark:border-border-dark"
      >
        <AppPagination
          :page="page"
          :page-size="pageSize"
          :total="total"
          @update:page="onPageChange"
        />
      </div>
    </section>

    <p v-if="loadError" class="text-sm text-danger">{{ loadError }}</p>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "default" });

const api = useApi();
const auth = useAuthStore();

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
}

const page = ref(1);
const pageSize = 20;
const loadError = ref("");

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

const users = computed(() => data.value?.content ?? []);
const total = computed(() => data.value?.totalElements ?? users.value.length);

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

const columns = [
  { key: "fullName", label: "Name" },
  { key: "email", label: "Email" },
  { key: "roleName", label: "Role" },
  { key: "status", label: "Status" },
];
</script>
