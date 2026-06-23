<script setup lang="ts">
interface TableColumn {
  key: string;
  label: string;
  class?: string;
  headerClass?: string;
  hideBelow?: "sm" | "md" | "lg" | "xl";
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

function hideBelowClass(col: TableColumn): string {
  const breakpoints: Record<string, string> = {
    sm: "hidden sm:table-cell",
    md: "hidden md:table-cell",
    lg: "hidden lg:table-cell",
    xl: "hidden xl:table-cell",
  };
  return col.hideBelow ? breakpoints[col.hideBelow] : "";
}
</script>

<template>
  <div class="w-full overflow-x-auto">
    <table
      class="w-full table-auto divide-y divide-border dark:divide-[var(--c-border)]"
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
              hideBelowClass(col),
            ]"
          >
            {{ col.label }}
          </th>
        </tr>
      </thead>

      <tbody class="divide-y divide-border dark:divide-[var(--c-border)]">
        <!-- Loading skeleton -->
        <template v-if="loading">
          <tr v-for="(_, i) in skeletonRows" :key="i" class="animate-pulse">
            <td v-for="col in columns" :key="col.key" :class="['px-6 py-4', hideBelowClass(col)]">
              <div class="h-4 rounded bg-gray-200 dark:bg-[var(--c-input)]" />
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
            class="hover:bg-gray-50 dark:hover:bg-[var(--c-input)]/50 transition-colors cursor-default"
            @click="$emit('rowClick', row)"
          >
            <td
              v-for="col in columns"
              :key="col.key"
              :class="[
                'px-6 py-4 text-sm text-gray-700 dark:text-gray-300 align-middle',
                col.class,
                hideBelowClass(col),
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
