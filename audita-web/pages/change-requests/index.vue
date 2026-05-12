<template>
  <div class="space-y-6">
    <!-- Page header -->
    <div
      class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4"
    >
      <div>
        <p
          class="text-xs text-primary/70 uppercase tracking-[0.16em] font-semibold mb-1"
        >
          Operations / Infrastructure
        </p>
        <h1
          class="text-3xl font-bold tracking-tight text-gray-900 dark:text-gray-100"
        >
          Change Requests
        </h1>
        <p class="text-sm text-muted mt-1 max-w-md">
          Managing and orchestrating infrastructure transitions with
          architectural precision.
        </p>
      </div>
      <div class="flex items-center gap-4 sm:text-right shrink-0">
        <div>
          <p class="text-xs text-muted uppercase tracking-wide">Open Changes</p>
          <p class="text-2xl font-bold text-primary">
            {{ page?.totalElements ?? 0 }}
          </p>
        </div>
        <button
          class="btn-primary btn-md shadow-lg shadow-primary/20"
          @click="navigateTo('/change-requests/new')"
        >
          Create Change
        </button>
      </div>
    </div>

    <!-- Filter pill + dropdown -->
    <div class="relative" ref="filterBarRef">
      <div class="flex items-center gap-2">
        <button
          class="btn-secondary btn-sm flex items-center gap-2 rounded-full px-4"
          :aria-expanded="filtersOpen"
          aria-controls="filter-panel"
          @click="filtersOpen = !filtersOpen"
        >
          <svg
            class="w-3.5 h-3.5 shrink-0"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"
            />
          </svg>
          Filters
          <span
            v-if="activeFilterCount > 0"
            class="ml-0.5 inline-flex items-center justify-center w-4 h-4 rounded-full bg-primary text-white text-[10px] font-bold leading-none"
          >
            {{ activeFilterCount }}
          </span>
          <svg
            class="w-3 h-3 transition-transform duration-150"
            :class="filtersOpen ? 'rotate-180' : ''"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>
        <button
          v-if="activeFilterCount > 0"
          class="btn-ghost btn-sm text-xs text-muted"
          @click="clearFilters"
        >
          ✕ Clear
        </button>
      </div>

      <!-- Dropdown filter panel -->
      <Transition
        enter-active-class="transition duration-100 ease-out"
        enter-from-class="opacity-0 -translate-y-1"
        enter-to-class="opacity-100 translate-y-0"
        leave-active-class="transition duration-75 ease-in"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 -translate-y-1"
      >
        <div
          v-if="filtersOpen"
          id="filter-panel"
          class="absolute top-full left-0 mt-2 z-20 bg-white dark:bg-slate-800 rounded-xl border border-outline-variant/50 dark:border-border-dark shadow-card-hover p-4 flex flex-col sm:flex-row gap-4 min-w-max"
        >
          <div class="flex items-center gap-2">
            <label
              for="filter-status"
              class="text-xs font-semibold text-muted uppercase whitespace-nowrap"
              >Status:</label
            >
            <select
              id="filter-status"
              v-model="filters.status"
              @change="load"
              class="input py-1.5 pr-8 text-sm w-40"
            >
              <option value="">All States</option>
              <option value="DRAFT">Draft</option>
              <option value="PENDING_APPROVAL">Pending Approval</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label
              for="filter-priority"
              class="text-xs font-semibold text-muted uppercase whitespace-nowrap"
              >Priority:</label
            >
            <select
              id="filter-priority"
              v-model="filters.priority"
              @change="load"
              class="input py-1.5 pr-8 text-sm w-36"
            >
              <option value="">Any Priority</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
        </div>
      </Transition>

      <!-- Screen-reader announcement of loading/filtering state -->
      <span class="sr-only" aria-live="polite" aria-atomic="true">{{
        liveAnnouncement
      }}</span>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden shadow-card-hover">
      <table class="w-full" aria-label="Change requests">
        <thead>
          <tr class="border-b border-border dark:border-border-dark">
            <th class="table-header px-4 py-3 text-left w-28" scope="col">
              ID
            </th>
            <th class="table-header px-4 py-3 text-left" scope="col">
              Change Title
            </th>
            <th class="table-header px-4 py-3 text-left w-36" scope="col">
              Status
            </th>
            <th class="table-header px-4 py-3 text-left w-24" scope="col">
              Priority
            </th>
            <th
              class="table-header px-4 py-3 text-left w-28 hidden lg:table-cell"
              scope="col"
            >
              Scheduled
            </th>
            <th
              class="table-header px-4 py-3 text-left w-28 hidden xl:table-cell"
              scope="col"
            >
              SLA Deadline
            </th>
            <th
              class="table-header px-4 py-3 text-left w-36 hidden lg:table-cell"
              scope="col"
            >
              Requester
            </th>
          </tr>
        </thead>
        <tbody>
          <!-- Skeleton loading rows -->
          <template v-if="isLoading">
            <tr v-for="n in 5" :key="`sk-${n}`" class="animate-pulse">
              <td class="px-4 py-4">
                <div class="h-3.5 w-20 bg-border dark:bg-border-dark rounded" />
              </td>
              <td class="px-4 py-4">
                <div
                  class="h-3.5 w-48 bg-border dark:bg-border-dark rounded mb-1.5"
                />
                <div class="h-2.5 w-24 bg-border dark:bg-border-dark rounded" />
              </td>
              <td class="px-4 py-4">
                <div
                  class="h-5 w-24 bg-border dark:bg-border-dark rounded-full"
                />
              </td>
              <td class="px-4 py-4">
                <div
                  class="h-5 w-16 bg-border dark:bg-border-dark rounded-full"
                />
              </td>
              <td class="px-4 py-4 hidden lg:table-cell">
                <div class="h-3 w-20 bg-border dark:bg-border-dark rounded" />
              </td>
              <td class="px-4 py-4 hidden xl:table-cell">
                <div class="h-3 w-20 bg-border dark:bg-border-dark rounded" />
              </td>
              <td class="px-4 py-4 hidden lg:table-cell">
                <div class="h-3 w-28 bg-border dark:bg-border-dark rounded" />
              </td>
            </tr>
          </template>

          <!-- Empty state -->
          <tr v-else-if="!crs.length">
            <td colspan="7" class="px-4 py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <div
                  class="w-14 h-14 rounded-2xl bg-surface-container-low dark:bg-slate-800 flex items-center justify-center"
                >
                  <svg
                    class="w-7 h-7 text-muted"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.5"
                      d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                    />
                  </svg>
                </div>
                <div>
                  <p class="text-sm font-semibold text-on-surface">
                    No change requests found
                  </p>
                  <p class="text-xs text-muted mt-1">
                    {{
                      filters.status || filters.priority
                        ? "Try adjusting your filters or clearing them to see all requests."
                        : "Create your first change request to get started."
                    }}
                  </p>
                </div>
                <button
                  v-if="filters.status || filters.priority"
                  class="btn-ghost btn-sm"
                  @click="clearFilters"
                >
                  Clear filters
                </button>
              </div>
            </td>
          </tr>

          <!-- Data rows -->
          <tr
            v-else
            v-for="cr in crs"
            :key="cr.id"
            class="table-row cursor-pointer"
            tabindex="0"
            :aria-label="`${cr.title} — ${cr.status}, ${cr.priority} priority`"
            @click="navigateTo(`/change-requests/${cr.id}`)"
            @keydown.enter="navigateTo(`/change-requests/${cr.id}`)"
            @keydown.space.prevent="navigateTo(`/change-requests/${cr.id}`)"
          >
            <td class="px-4 py-4">
              <span class="font-mono text-xs text-primary font-semibold"
                >CHG-{{ cr.id.slice(0, 8).toUpperCase() }}</span
              >
            </td>
            <td class="px-4 py-4">
              <p class="font-semibold text-sm text-gray-900 dark:text-gray-100">
                {{ cr.title }}
              </p>
              <p class="text-xs text-muted mt-0.5">{{ cr.category }}</p>
            </td>
            <td class="px-4 py-4">
              <CrStatusBadge :status="cr.status" />
            </td>
            <td class="px-4 py-4">
              <CrPriorityBadge :priority="cr.priority" />
            </td>
            <td class="px-4 py-4 text-xs text-muted hidden lg:table-cell">
              {{ cr.scheduledStart ? formatDate(cr.scheduledStart) : "—" }}
            </td>
            <td class="px-4 py-4 text-xs hidden xl:table-cell">
              <span
                v-if="cr.slaDeadline"
                :class="
                  isSlaOverdue(cr.slaDeadline)
                    ? 'text-danger font-semibold'
                    : 'text-muted'
                "
              >
                {{ formatDate(cr.slaDeadline) }}
              </span>
              <span v-else class="text-muted">—</span>
            </td>
            <td class="px-4 py-4 hidden lg:table-cell">
              <div class="flex items-center gap-2">
                <div
                  class="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center text-primary text-xs font-bold uppercase"
                  aria-hidden="true"
                >
                  {{ initials(cr.createdByFullName) }}
                </div>
                <span class="text-xs text-gray-700 dark:text-gray-300">{{
                  cr.createdByFullName ?? "Unknown"
                }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div
        v-if="page && page.totalPages > 1"
        class="px-4 py-3 border-t border-border dark:border-border-dark flex items-center justify-between gap-3"
      >
        <p class="text-xs text-muted">
          Showing {{ page.number * page.size + 1 }}–{{
            Math.min((page.number + 1) * page.size, page.totalElements)
          }}
          of {{ page.totalElements }} entries
        </p>
        <div class="flex items-center gap-2">
          <button
            @click="setPage(currentPage - 1)"
            :disabled="currentPage === 0 || isLoading"
            class="btn-ghost btn-sm px-2.5 min-w-[2rem]"
            aria-label="Previous page"
          >
            ‹
          </button>
          <span class="text-xs text-muted px-1"
            >Page {{ currentPage + 1 }} of {{ page.totalPages }}</span
          >
          <button
            @click="setPage(currentPage + 1)"
            :disabled="currentPage >= page.totalPages - 1 || isLoading"
            class="btn-ghost btn-sm px-2.5 min-w-[2rem]"
            aria-label="Next page"
          >
            ›
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChangeRequest, Page } from "~/types";
import { format, parseISO, isPast } from "date-fns";

definePageMeta({ middleware: "auth" });

useHead({ title: "Change Requests — Audita" });

const { list } = useChangeRequests();
const PAGE_SIZE = 50;

const filters = reactive({ status: "", priority: "" });
const currentPage = ref(0);
const isLoading = ref(false);
const page = ref<Page<ChangeRequest> | null>(null);
const crs = computed(() => page.value?.content ?? []);
const liveAnnouncement = ref("");

const filtersOpen = ref(false);
const filterBarRef = ref<HTMLElement | null>(null);
const activeFilterCount = computed(
  () => (filters.status ? 1 : 0) + (filters.priority ? 1 : 0),
);

function onDocumentClick(e: MouseEvent) {
  if (filterBarRef.value && !filterBarRef.value.contains(e.target as Node)) {
    filtersOpen.value = false;
  }
}

async function fetchPage(pageIndex: number) {
  isLoading.value = true;
  liveAnnouncement.value = "Loading change requests…";
  try {
    const nextPage = await list({
      status: filters.status || undefined,
      priority: filters.priority || undefined,
      page: pageIndex,
      size: PAGE_SIZE,
      sort: "createdAt,desc",
    });

    page.value = nextPage;
    currentPage.value = pageIndex;
    liveAnnouncement.value = `Loaded ${nextPage.totalElements} change requests.`;
  } finally {
    isLoading.value = false;
  }
}

function load() {
  return fetchPage(0);
}

function setPage(p: number) {
  if (!page.value || p < 0 || p >= page.value.totalPages || isLoading.value) {
    return;
  }
  return fetchPage(p);
}

function clearFilters() {
  filters.status = "";
  filters.priority = "";
  load();
}

function formatDate(iso: string) {
  return format(parseISO(iso), "MMM d, yyyy");
}

function isSlaOverdue(iso: string) {
  return isPast(parseISO(iso));
}

function initials(name: string | null) {
  if (!name) {
    return "NA";
  }
  return name
    .split(" ")
    .slice(0, 2)
    .map((n) => n.charAt(0).toUpperCase())
    .join("");
}

onMounted(() => {
  load();
  document.addEventListener("click", onDocumentClick);
});

onUnmounted(() => {
  document.removeEventListener("click", onDocumentClick);
});
</script>
