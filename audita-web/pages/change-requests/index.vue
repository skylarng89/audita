<template>
  <div class="space-y-6">
    <div class="flex items-start justify-between">
      <div>
        <p
          class="text-xs text-primary/70 uppercase tracking-[0.16em] font-semibold mb-1"
        >
          Operations / Infrastructure
        </p>
        <h1
          class="text-4xl font-bold tracking-tight text-gray-900 dark:text-gray-100"
        >
          Change Requests
        </h1>
        <p class="text-sm text-muted mt-1 max-w-md">
          Managing and orchestrating infrastructure transitions with
          architectural precision.
        </p>
      </div>
      <div class="flex items-center gap-6 text-right">
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

    <div class="card p-4 flex flex-wrap gap-3 items-center shadow-card-hover">
      <div class="flex items-center gap-2">
        <label class="text-xs font-semibold text-muted uppercase"
          >Status:</label
        >
        <select
          v-model="filters.status"
          @change="load"
          class="input py-1.5 pr-8 text-sm w-36"
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
        <label class="text-xs font-semibold text-muted uppercase"
          >Priority:</label
        >
        <select
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

    <div class="card overflow-hidden shadow-card-hover">
      <table class="w-full">
        <thead>
          <tr class="border-b border-border dark:border-border-dark">
            <th class="table-header px-4 py-3 text-left w-28">ID</th>
            <th class="table-header px-4 py-3 text-left">Change Title</th>
            <th class="table-header px-4 py-3 text-left w-36">Status</th>
            <th class="table-header px-4 py-3 text-left w-24">Priority</th>
            <th
              class="table-header px-4 py-3 text-left w-28 hidden lg:table-cell"
            >
              Scheduled
            </th>
            <th
              class="table-header px-4 py-3 text-left w-36 hidden lg:table-cell"
            >
              Requester
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="isLoading && !crs.length">
            <td colspan="6" class="px-4 py-12 text-center text-sm text-muted">
              Loading…
            </td>
          </tr>
          <tr v-else-if="!isLoading && !crs.length">
            <td colspan="6" class="px-4 py-12 text-center text-sm text-muted">
              No change requests found.
            </td>
          </tr>
          <tr
            v-else
            v-for="cr in crs"
            :key="cr.id"
            class="table-row"
            @click="navigateTo(`/change-requests/${cr.id}`)"
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
            <td class="px-4 py-4 hidden lg:table-cell">
              <div class="flex items-center gap-2">
                <div
                  class="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center text-primary text-xs font-bold uppercase"
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
            class="btn-ghost btn-sm px-2"
          >
            &lsaquo;
          </button>
          <span class="text-xs text-muted"
            >Page {{ currentPage + 1 }} of {{ page.totalPages }}</span
          >
          <button
            @click="setPage(currentPage + 1)"
            :disabled="currentPage >= page.totalPages - 1 || isLoading"
            class="btn-ghost btn-sm px-2"
          >
            &rsaquo;
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChangeRequest, Page } from "~/types";
import { format, parseISO } from "date-fns";

definePageMeta({ middleware: "auth" });

const { list } = useChangeRequests();
const PAGE_SIZE = 50;

const filters = reactive({ status: "", priority: "" });
const currentPage = ref(0);
const isLoading = ref(false);
const page = ref<Page<ChangeRequest> | null>(null);
const crs = computed(() => page.value?.content ?? []);

async function fetchPage(pageIndex: number) {
  isLoading.value = true;
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

function formatDate(iso: string) {
  return format(parseISO(iso), "MMM d, yyyy");
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

onMounted(load);
</script>
