<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          IAM Framework
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Groups</h1>
        <p class="text-sm text-muted mt-1">
          Organize review and approval ownership by team.
        </p>
      </div>
      <NuxtLink
        to="/admin/groups"
        class="btn-primary btn-sm shadow-lg shadow-primary/20"
      >
        Open Admin Groups
      </NuxtLink>
    </div>

    <section class="card shadow-card-hover">
      <AppTable
        :columns="columns"
        :data="groups"
        :loading="pending"
        row-key="id"
        empty-message="No data returned."
      >
        <template #cell-description="{ value }">
          <span class="text-sm text-muted">{{
            value || "No data returned."
          }}</span>
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

interface GroupRow {
  id: string;
  name: string;
  description?: string;
}

interface GroupPayload {
  content?: GroupRow[];
  totalElements?: number;
}

const page = ref(1);
const pageSize = 20;
const loadError = ref("");

const { data, pending, refresh } = await useAsyncData<GroupPayload>(
  "groups-index-page",
  async () => {
    loadError.value = "";

    try {
      return (await api(
        `/api/v1/groups?page=${page.value - 1}&size=${pageSize}`,
      )) as GroupPayload;
    } catch {
      loadError.value = "No data returned.";
      return { content: [], totalElements: 0 };
    }
  },
  { watch: [page] },
);

const groups = computed(() => data.value?.content ?? []);
const total = computed(() => data.value?.totalElements ?? groups.value.length);

function onPageChange(nextPage: number) {
  page.value = nextPage;
  refresh();
}

const columns = [
  { key: "name", label: "Group" },
  { key: "description", label: "Description" },
];
</script>
