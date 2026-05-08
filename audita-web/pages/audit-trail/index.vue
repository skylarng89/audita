<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Governance and Compliance
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Audit Trail</h1>
        <p class="text-sm text-muted mt-1">
          Trace identity, policy, and workflow activity across your workspace.
        </p>
      </div>
      <button
        type="button"
        class="btn-secondary btn-sm"
        :disabled="exporting"
        :aria-label="
          exporting ? 'Preparing export…' : 'Export audit log as CSV'
        "
        @click="exportCsv"
      >
        {{ exporting ? "Exporting…" : "Export CSV" }}
      </button>
    </div>

    <!-- Filters -->
    <section class="card p-5 shadow-card-hover">
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <div>
          <label class="field-label" for="audit-action-type">Action Type</label>
          <select
            id="audit-action-type"
            v-model="filters.actionType"
            class="input"
          >
            <option value="">All Actions</option>
            <option v-for="at in ACTION_TYPES" :key="at" :value="at">
              {{ at }}
            </option>
          </select>
        </div>
        <div>
          <label class="field-label" for="audit-entity-type">Entity Type</label>
          <select
            id="audit-entity-type"
            v-model="filters.entityType"
            class="input"
          >
            <option value="">All Entities</option>
            <option value="change_request">Change Request</option>
            <option value="user">User</option>
            <option value="group">Group</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="audit-actor">Actor Email</label>
          <input
            id="audit-actor"
            v-model="filters.actorEmail"
            type="text"
            class="input"
            placeholder="Search by email"
            @input="onFilterInput"
          />
        </div>
        <div>
          <label class="field-label" for="audit-from">From</label>
          <input
            id="audit-from"
            v-model="filters.from"
            type="date"
            class="input"
          />
        </div>
        <div>
          <label class="field-label" for="audit-to">To</label>
          <input id="audit-to" v-model="filters.to" type="date" class="input" />
        </div>
      </div>
    </section>

    <!-- Table -->
    <section class="card shadow-card-hover overflow-hidden">
      <SharedAppTable
        :columns="columns"
        :data="rows"
        :loading="pending"
        row-key="id"
        empty-message="No audit log entries match the current filters."
      >
        <template #cell-createdAt="{ value }">
          <span class="text-xs text-muted font-mono">{{
            formatTs(value as string)
          }}</span>
        </template>
        <template #cell-actionType="{ value }">
          <AppBadge :variant="actionVariant(value as string)">{{
            value
          }}</AppBadge>
        </template>
        <template #cell-entityType="{ value }">
          <span class="text-xs capitalize">{{ value ?? "—" }}</span>
        </template>
        <template #cell-actorEmail="{ value }">
          <span class="text-sm">{{ value ?? "System" }}</span>
        </template>
        <template #cell-entityId="{ value }">
          <span
            class="text-xs font-mono text-muted truncate block max-w-[160px]"
            >{{ value ?? "—" }}</span
          >
        </template>
      </SharedAppTable>
    </section>

    <!-- Pagination -->
    <SharedAppPagination
      v-if="totalElements > pageSize"
      :page="page"
      :page-size="pageSize"
      :total="totalElements"
      @update:page="onPageChange"
    />

    <p v-if="loadError" class="text-sm text-danger" role="alert">
      {{ loadError }}
    </p>
  </div>
</template>

<script setup lang="ts">
import type { AuditLogEntry, Page } from "~/types";

definePageMeta({ layout: "default" });

const api = useApi();

// ── Constants ─────────────────────────────────────────────────────────────────

const ACTION_TYPES = [
  "CR_CREATED",
  "CR_UPDATED",
  "CR_SUBMITTED",
  "CR_APPROVED",
  "CR_REJECTED",
  "CR_CANCELLED",
  "CR_APPROVER_ADDED",
  "CR_APPROVER_REMOVED",
  "CR_APPROVERS_REORDERED",
  "CR_ATTACHMENT_UPLOADED",
  "CR_CUSTOM_FIELDS_UPDATED",
  "SLA_WARNING",
  "SLA_BREACH",
];

const columns = [
  { key: "createdAt", label: "Timestamp" },
  { key: "actorEmail", label: "Actor" },
  { key: "actionType", label: "Action" },
  { key: "entityType", label: "Entity Type" },
  { key: "entityId", label: "Entity ID" },
];

// ── State ─────────────────────────────────────────────────────────────────────

const page = ref(1);
const pageSize = 20;
const totalElements = ref(0);
const loadError = ref("");
const exporting = ref(false);
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const filters = reactive({
  actionType: "",
  entityType: "",
  actorEmail: "",
  from: "",
  to: "",
});

// ── Data fetching ─────────────────────────────────────────────────────────────

const { data, pending, refresh } = await useAsyncData<Page<AuditLogEntry>>(
  "audit-trail-page",
  async () => {
    loadError.value = "";
    try {
      const query: Record<string, string | number> = {
        page: page.value - 1,
        size: pageSize,
        sort: "createdAt,desc",
      };
      if (filters.actionType) query.actionType = filters.actionType;
      if (filters.entityType) query.entityType = filters.entityType;
      if (filters.actorEmail) query.actorEmail = filters.actorEmail;
      if (filters.from) query.from = filters.from;
      if (filters.to) query.to = filters.to;

      return await api<Page<AuditLogEntry>>("/api/v1/audit-trail", { query });
    } catch {
      loadError.value = "Unable to load audit trail entries.";
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: pageSize,
        number: 0,
      };
    }
  },
  {
    watch: [
      page,
      () => filters.actionType,
      () => filters.entityType,
      () => filters.from,
      () => filters.to,
    ],
  },
);

const rows = computed<Record<string, unknown>[]>(
  () => (data.value?.content ?? []) as unknown as Record<string, unknown>[],
);

watch(data, (val) => {
  if (val) totalElements.value = val.totalElements;
});

// ── Handlers ──────────────────────────────────────────────────────────────────

function onFilterInput() {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    page.value = 1;
    refresh();
  }, 350);
}

function onPageChange(newPage: number) {
  page.value = newPage;
}

async function exportCsv() {
  exporting.value = true;
  try {
    const query: Record<string, string> = {};
    if (filters.actionType) query.actionType = filters.actionType;
    if (filters.entityType) query.entityType = filters.entityType;
    if (filters.actorEmail) query.actorEmail = filters.actorEmail;
    if (filters.from) query.from = filters.from;
    if (filters.to) query.to = filters.to;

    const params = new URLSearchParams(query).toString();
    const url = `/api/v1/audit-trail/export.csv${params ? `?${params}` : ""}`;

    const response = await $fetch.raw(url, { responseType: "blob" });
    const blob = response._data as Blob;
    const anchor = document.createElement("a");
    anchor.href = URL.createObjectURL(blob);
    anchor.download = "audit-trail.csv";
    anchor.click();
    URL.revokeObjectURL(anchor.href);
  } catch {
    loadError.value = "Export failed. Please try again.";
  } finally {
    exporting.value = false;
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatTs(ts: string): string {
  if (!ts) return "—";
  try {
    return new Intl.DateTimeFormat("en-GB", {
      year: "numeric",
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      timeZoneName: "short",
    }).format(new Date(ts));
  } catch {
    return ts;
  }
}

function actionVariant(
  action: string,
): "success" | "danger" | "warning" | "neutral" | "info" {
  if (
    action?.includes("REJECTED") ||
    action?.includes("CANCELLED") ||
    action?.includes("BREACH")
  ) {
    return "danger";
  }
  if (action?.includes("WARNING") || action?.includes("REMOVED")) {
    return "warning";
  }
  if (action?.includes("APPROVED")) {
    return "success";
  }
  if (action?.includes("SUBMITTED") || action?.includes("CREATED")) {
    return "info";
  }
  return "neutral";
}
</script>
