<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold">Command Centre</h1>
        <p class="text-sm text-muted mt-1">
          Platform-wide overview across all tenants.
        </p>
      </div>
      <NuxtLink to="/platform/tenants/new" class="btn-primary btn-sm">
        + Provision New Org
      </NuxtLink>
    </div>

    <!-- Stats grid -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
      <div class="card p-5">
        <p
          class="text-xs font-semibold text-muted uppercase tracking-wide mb-1"
        >
          Total Organisations
        </p>
        <p class="text-3xl font-bold">{{ stats.total }}</p>
      </div>
      <div class="card p-5">
        <p
          class="text-xs font-semibold text-muted uppercase tracking-wide mb-1"
        >
          Active
        </p>
        <p class="text-3xl font-bold text-success">{{ stats.active }}</p>
      </div>
      <div class="card p-5">
        <p
          class="text-xs font-semibold text-muted uppercase tracking-wide mb-1"
        >
          Suspended
        </p>
        <p class="text-3xl font-bold text-danger">{{ stats.suspended }}</p>
      </div>
    </div>

    <!-- Recent tenants -->
    <div class="card">
      <div
        class="flex items-center justify-between px-5 py-4 border-b border-border dark:border-border-dark"
      >
        <h2 class="font-semibold">Recent Organisations</h2>
        <NuxtLink
          to="/platform/tenants"
          class="text-xs text-primary hover:underline"
          >View all →</NuxtLink
        >
      </div>
      <AppTable :columns="columns" :rows="recentTenants" :loading="pending">
        <template #cell-status="{ row }">
          <AppBadge :variant="row.status === 'ACTIVE' ? 'success' : 'danger'">{{
            row.status
          }}</AppBadge>
        </template>
        <template #cell-actions="{ row }">
          <NuxtLink
            :to="`/platform/tenants/${row.id}`"
            class="text-xs text-primary hover:underline"
          >
            Manage →
          </NuxtLink>
        </template>
      </AppTable>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "platform" });

const api = useApi();

interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: "ACTIVE" | "SUSPENDED";
  createdAt: string;
}

interface PageData {
  content: Tenant[];
  totalElements: number;
}

const { data, pending } = await useAsyncData<PageData>(
  "platform-tenants",
  () =>
    api("/api/platform/v1/tenants?size=5", {
      method: "GET",
    }) as Promise<PageData>,
);

const recentTenants = computed(() => data.value?.content ?? []);

const stats = computed(() => {
  const all = data.value?.content ?? [];
  return {
    total: data.value?.totalElements ?? 0,
    active: all.filter((t) => t.status === "ACTIVE").length,
    suspended: all.filter((t) => t.status === "SUSPENDED").length,
  };
});

const columns = [
  { key: "name", label: "Name" },
  { key: "slug", label: "Slug" },
  { key: "status", label: "Status" },
  { key: "createdAt", label: "Created" },
  { key: "actions", label: "" },
];
</script>
