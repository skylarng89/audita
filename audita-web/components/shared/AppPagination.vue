<script setup lang="ts">
const props = defineProps<{
  page: number;
  pageSize: number;
  total: number;
}>();

const emit = defineEmits<{
  "update:page": [page: number];
}>();

const totalPages = computed(() => Math.ceil(props.total / props.pageSize));

const pageNumbers = computed<(number | "...")[]>(() => {
  const total = totalPages.value;
  const current = props.page;

  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  if (current <= 4) {
    return [1, 2, 3, 4, 5, "...", total];
  }

  if (current >= total - 3) {
    return [1, "...", total - 4, total - 3, total - 2, total - 1, total];
  }

  return [1, "...", current - 1, current, current + 1, "...", total];
});

const rangeStart = computed(() => (props.page - 1) * props.pageSize + 1);
const rangeEnd = computed(() =>
  Math.min(props.page * props.pageSize, props.total),
);
</script>

<template>
  <div class="flex items-center justify-between gap-4 py-3">
    <!-- Range summary -->
    <p class="text-sm text-muted shrink-0">
      Showing
      <span class="font-medium text-gray-700 dark:text-gray-300"
        >{{ rangeStart }}–{{ rangeEnd }}</span
      >
      of
      <span class="font-medium text-gray-700 dark:text-gray-300">{{
        total
      }}</span>
    </p>

    <!-- Page controls -->
    <nav class="flex items-center gap-1" aria-label="Pagination">
      <!-- Previous -->
      <button
        type="button"
        :disabled="page === 1"
        class="flex h-8 w-8 items-center justify-center rounded-lg border border-outline-variant text-muted transition-colors hover:bg-surface-container disabled:opacity-40 disabled:cursor-not-allowed dark:border-[var(--c-border)] dark:hover:bg-[var(--c-input)]"
        aria-label="Previous page"
        @click="emit('update:page', page - 1)"
      >
        <svg
          class="h-4 w-4"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M15 19l-7-7 7-7"
          />
        </svg>
      </button>

      <span class="text-xs text-muted sm:hidden">Page {{ page }} of {{ totalPages }}</span>

      <!-- Page numbers -->
      <template v-for="(p, i) in pageNumbers" :key="i">
        <span
          v-if="p === '...'"
          class="hidden sm:flex h-8 w-8 items-center justify-center text-sm text-muted"
        >
          &hellip;
        </span>
        <button
          v-else
          type="button"
          :aria-current="p === page ? 'page' : undefined"
          :class="[
            'hidden sm:inline-flex h-8 w-8 items-center justify-center rounded text-sm font-medium transition-colors',
            p === page
              ? 'bg-primary text-white shadow-sm'
              : 'border border-outline-variant text-on-surface-variant hover:bg-surface-container dark:border-[var(--c-border)] dark:text-gray-300 dark:hover:bg-[var(--c-input)]',
          ]"
          @click="emit('update:page', p as number)"
        >
          {{ p }}
        </button>
      </template>

      <!-- Next -->
      <button
        type="button"
        :disabled="page === totalPages"
        class="flex h-8 w-8 items-center justify-center rounded-lg border border-outline-variant text-muted transition-colors hover:bg-surface-container disabled:opacity-40 disabled:cursor-not-allowed dark:border-[var(--c-border)] dark:hover:bg-[var(--c-input)]"
        aria-label="Next page"
        @click="emit('update:page', page + 1)"
      >
        <svg
          class="h-4 w-4"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M9 5l7 7-7 7"
          />
        </svg>
      </button>
    </nav>
  </div>
</template>
