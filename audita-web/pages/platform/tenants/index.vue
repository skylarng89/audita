<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold">Tenant Management</h1>
        <p class="text-sm text-muted mt-1">
          Provision and manage all organisations on the platform.
        </p>
      </div>
      <NuxtLink to="/platform/tenants/new" class="btn-primary btn-sm">
        + Provision New Org
      </NuxtLink>
    </div>

    <div class="card">
      <AppTable :columns="columns" :rows="tenants" :loading="pending">
        <template #cell-status="{ row }">
          <AppBadge :variant="row.status === 'ACTIVE' ? 'success' : 'danger'">{{
            row.status
          }}</AppBadge>
        </template>
        <template #cell-createdAt="{ row }">
          {{ formatDate(row.createdAt) }}
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

      <div class="px-5 py-4 border-t border-border dark:border-border-dark">
        <AppPagination
          :current-page="page"
          :total-pages="totalPages"
          @change="onPageChange"
        />
      </div>
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
  totalPages: number;
  number: number;
}

const page = ref(0);

const { data, pending, refresh } = await useAsyncData<PageData>(
  "platform-tenants-list",
  () =>
    api(`/api/platform/v1/tenants?page=${page.value}&size=20`, {
      method: "GET",
    }) as Promise<PageData>,
);

watch(page, () => refresh());

const tenants = computed(() => data.value?.content ?? []);
const totalPages = computed(() => data.value?.totalPages ?? 1);

function onPageChange(newPage: number) {
  page.value = newPage;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-GB", { dateStyle: "medium" });
}

const columns = [
  { key: "name", label: "Name" },
  { key: "slug", label: "Slug" },
  { key: "status", label: "Status" },
  { key: "createdAt", label: "Created" },
  { key: "actions", label: "" },
];
</script>
