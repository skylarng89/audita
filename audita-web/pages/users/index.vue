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
          Manage account access, role assignment, and operational status.
        </p>
      </div>
      <NuxtLink
        to="/admin/users"
        class="btn-primary btn-sm shadow-lg shadow-primary/20"
      >
        Open Admin Users
      </NuxtLink>
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
        empty-message="No data returned."
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

      <div class="border-t border-border px-5 py-4 dark:border-border-dark">
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

    try {
      return (await api(
        `/api/v1/users?page=${page.value - 1}&size=${pageSize}`,
      )) as UserPayload;
    } catch {
      loadError.value = "No data returned.";
      return { content: [], totalElements: 0 };
    }
  },
  { watch: [page] },
);

const users = computed(() => data.value?.content ?? []);
const total = computed(() => data.value?.totalElements ?? users.value.length);

const totalUsers = computed(() => users.value.length);
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
