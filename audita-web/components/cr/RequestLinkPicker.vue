<template>
  <div class="relative" ref="wrapperRef">
    <div
      class="input mt-1 flex flex-wrap gap-1 min-h-[2.5rem] cursor-text"
      :class="{ 'ring-2 ring-primary': dropdownOpen }"
      @click="focusInput"
    >
      <span
        v-for="item in selectedItems"
        :key="item.id"
        class="inline-flex items-center gap-1 rounded bg-primary/10 text-primary text-xs px-2 py-0.5 shrink-0"
      >
        {{ item.displayId }} — {{ item.title }}
        <button
          type="button"
          class="hover:text-danger leading-none"
          :aria-label="`Remove ${item.displayId}`"
          @click.stop="removeItem(item.id)"
        >
          ×
        </button>
      </span>
      <input
        ref="searchInputRef"
        v-model="searchQuery"
        class="flex-1 min-w-[10rem] bg-transparent outline-none text-sm"
        placeholder="Search requests to link…"
        @focus="onFocus"
        @input="onSearchInput"
      />
    </div>

    <ul
      v-if="dropdownOpen && searchResults.length"
      class="absolute z-50 mt-1 w-full bg-surface border border-border dark:border-[var(--c-border)] rounded-lg shadow-lg max-h-52 overflow-y-auto"
    >
      <li
        v-for="result in searchResults"
        :key="result.id"
        class="px-3 py-2 text-sm cursor-pointer hover:bg-primary/10 flex items-center justify-between gap-2"
        @mousedown.prevent="selectItem(result)"
      >
        <div class="flex-1 min-w-0 truncate">
          <span class="font-medium">{{ result.displayId }}</span>
          <span class="text-muted ml-1">{{ result.title }}</span>
        </div>
        <CrStatusBadge :status="result.status" />
      </li>
    </ul>

    <p
      v-if="dropdownOpen && !searchResults.length && searchQuery.trim() && !isSearching"
      class="absolute z-50 mt-1 w-full bg-surface border border-border dark:border-[var(--c-border)] rounded-lg shadow-lg px-3 py-2 text-sm text-muted"
    >
      No matching requests found.
    </p>
  </div>
</template>

<script setup lang="ts">
import type { ChangeRequestSearchResult } from "~/composables/useChangeRequests";

const props = defineProps<{
  modelValue: string[];
  currentRequestId?: string;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: string[]];
}>();

const { searchRequests } = useChangeRequests();

const wrapperRef = ref<HTMLElement | null>(null);
const searchInputRef = ref<HTMLInputElement | null>(null);
const searchQuery = ref("");
const searchResults = ref<ChangeRequestSearchResult[]>([]);
const selectedItems = ref<ChangeRequestSearchResult[]>([]);
const dropdownOpen = ref(false);
const isSearching = ref(false);

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

function focusInput() {
  searchInputRef.value?.focus();
}

function onFocus() {
  dropdownOpen.value = true;
}

function onSearchInput() {
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
  const query = searchQuery.value.trim();
  if (!query) {
    searchResults.value = [];
    return;
  }
  debounceTimer = setTimeout(() => performSearch(query), 300);
}

async function performSearch(query: string) {
  isSearching.value = true;
  try {
    const results = await searchRequests(query);
    searchResults.value = results.filter(
      (r) =>
        r.id !== props.currentRequestId &&
        !props.modelValue.includes(r.id),
    );
  } catch {
    searchResults.value = [];
  } finally {
    isSearching.value = false;
  }
}

function selectItem(item: ChangeRequestSearchResult) {
  if (props.modelValue.includes(item.id)) {
    return;
  }
  const updated = [...props.modelValue, item.id];
  emit("update:modelValue", updated);
  if (!selectedItems.value.find((i) => i.id === item.id)) {
    selectedItems.value = [...selectedItems.value, item];
  }
  searchQuery.value = "";
  searchResults.value = [];
}

function removeItem(id: string) {
  const updated = props.modelValue.filter((v) => v !== id);
  emit("update:modelValue", updated);
  selectedItems.value = selectedItems.value.filter((i) => i.id !== id);
}

watch(
  () => props.modelValue,
  async (ids) => {
    const missing = ids.filter(
      (id) => !selectedItems.value.find((i) => i.id === id),
    );
    if (missing.length) {
      for (const id of missing) {
        try {
          const results = await searchRequests(id, 1);
          const found = results.find((r) => r.id === id);
          if (found && !selectedItems.value.find((i) => i.id === id)) {
            selectedItems.value = [...selectedItems.value, found];
          }
        } catch {
          // ignore
        }
      }
    }
  },
  { immediate: true },
);

onMounted(() => {
  document.addEventListener("click", (e) => {
    if (wrapperRef.value && !wrapperRef.value.contains(e.target as Node)) {
      dropdownOpen.value = false;
    }
  });
});

onBeforeUnmount(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
});
</script>
