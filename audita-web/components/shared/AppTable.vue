<script setup lang="ts">
interface TableColumn {
  key: string;
  label: string;
  class?: string;
  headerClass?: string;
  render?: (row: Record<string, unknown>) => string;
}

const props = defineProps<{
  columns: TableColumn[];
  data: Record<string, unknown>[];
  loading?: boolean;
  emptyMessage?: string;
  rowKey?: string;
}>();

defineEmits<{
  rowClick: [row: Record<string, unknown>];
}>();

const skeletonRows = Array.from({ length: 5 });
</script>

<template>
  <div class="w-full overflow-x-auto">
    <table
      class="w-full min-w-[980px] table-auto divide-y divide-border dark:divide-border-dark"
    >
      <thead>
        <tr>
          <th
            v-for="col in columns"
            :key="col.key"
            scope="col"
            :class="[
              'px-6 py-4 text-left text-xs font-semibold uppercase tracking-wide text-muted whitespace-nowrap',
              col.headerClass,
            ]"
          >
            {{ col.label }}
          </th>
        </tr>
      </thead>

      <tbody class="divide-y divide-border dark:divide-border-dark">
        <!-- Loading skeleton -->
        <template v-if="loading">
          <tr v-for="(_, i) in skeletonRows" :key="i" class="animate-pulse">
            <td v-for="col in columns" :key="col.key" class="px-6 py-4">
              <div class="h-4 rounded bg-gray-200 dark:bg-slate-700" />
            </td>
          </tr>
        </template>

        <!-- Empty state -->
        <tr v-else-if="data.length === 0">
          <td
            :colspan="columns.length"
            class="px-6 py-12 text-center text-sm text-muted"
          >
            {{ emptyMessage ?? "No data to display." }}
          </td>
        </tr>

        <!-- Data rows -->
        <template v-else>
          <tr
            v-for="row in data"
            :key="rowKey ? String(row[rowKey]) : JSON.stringify(row)"
            class="hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors cursor-default"
            @click="$emit('rowClick', row)"
          >
            <td
              v-for="col in columns"
              :key="col.key"
              :class="[
                'px-6 py-4 text-sm text-gray-700 dark:text-gray-300 align-middle',
                col.class,
              ]"
            >
              <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
                {{ col.render ? col.render(row) : row[col.key] }}
              </slot>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </div>
</template>
