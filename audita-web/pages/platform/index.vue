<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Platform Scale
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Command Centre</h1>
        <p class="text-sm text-muted mt-1">
          Platform-wide overview across all tenants.
        </p>
      </div>
      <NuxtLink
        to="/platform/tenants/new"
        class="btn-primary btn-sm shadow-lg shadow-primary/20"
      >
        + Provision New Org
      </NuxtLink>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <div class="card p-5 shadow-card-hover">
        <p
          class="text-xs font-semibold text-muted uppercase tracking-wide mb-1"
        >
          Total Organisations
        </p>
        <p class="text-3xl font-bold">{{ stats.total }}</p>
      </div>
      <div class="card p-5 shadow-card-hover">
        <p
          class="text-xs font-semibold text-muted uppercase tracking-wide mb-1"
        >
          Active
        </p>
        <p class="text-3xl font-bold text-success">{{ stats.active }}</p>
      </div>
      <div
        class="card p-5 border-primary/20 bg-primary text-white shadow-card-hover"
      >
        <p
          class="text-xs font-semibold text-white/70 uppercase tracking-wide mb-1"
        >
          System Health
        </p>
        <p class="text-3xl font-bold">{{ health.availabilityPercent }}%</p>
        <p class="text-xs text-white/70 mt-1">{{ health.detail }}</p>
      </div>
    </div>

    <div class="card shadow-card-hover">
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

import { useLoadingOverlay } from "~/composables/useLoadingOverlay";

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

interface PlatformHealth {
  status: string;
  availabilityPercent: number;
  detail: string;
}

const { data, pending } = await useAsyncData<PageData>(
  "platform-tenants",
  () =>
    api("/api/platform/v1/tenants?size=5", {
      method: "GET",
    }) as Promise<PageData>,
);

const { data: healthData } = await useAsyncData<PlatformHealth>(
  "platform-health",
  () =>
    api("/api/platform/v1/health", {
      method: "GET",
    }) as Promise<PlatformHealth>,
);

const { hide: hideLoading } = useLoadingOverlay();
watch(pending, (val) => { if (!val) hideLoading(); });

const recentTenants = computed(() => data.value?.content ?? []);
const health = computed(() => ({
  status: healthData.value?.status ?? "UNKNOWN",
  availabilityPercent: healthData.value?.availabilityPercent ?? 0,
  detail: healthData.value?.detail ?? "Health data unavailable",
}));

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
