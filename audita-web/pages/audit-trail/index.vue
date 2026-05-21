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
        v-if="canExport"
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
          <label class="field-label" for="audit-range">Date Range</label>
          <select id="audit-range" v-model="filters.rangePreset" class="input">
            <option value="7d">Last 7 days</option>
            <option value="14d">Last 2 weeks</option>
            <option value="30d">Last 1 month</option>
            <option value="90d">Last 3 months</option>
            <option value="180d">Last 6 months</option>
            <option value="365d">Last 12 months</option>
            <option value="custom">Custom range</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="audit-action-type">Action Type</label>
          <select
            id="audit-action-type"
            v-model="filters.actionType"
            class="input"
          >
            <option value="">All Actions</option>
            <option v-for="at in ACTION_TYPES" :key="at" :value="at">
              {{ actionLabel(at) }}
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
           <label class="field-label" for="audit-actor">Actor</label>
          <input
            id="audit-actor"
            v-model="filters.actorEmail"
            type="text"
            class="input"
            placeholder="Search by name or email"
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
            :disabled="filters.rangePreset !== 'custom'"
          />
        </div>
        <div>
          <label class="field-label" for="audit-to">To</label>
          <input
            id="audit-to"
            v-model="filters.to"
            type="date"
            class="input"
            :disabled="filters.rangePreset !== 'custom'"
          />
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
            actionLabel(value as string)
          }}</AppBadge>
        </template>
        <template #cell-entityType="{ value }">
          <span class="text-xs">{{ entityLabel(value as string | null) }}</span>
        </template>
        <template #cell-actor="{ row }">
          <span class="text-sm">{{ actorLabel(row as Record<string, unknown>) }}</span>
        </template>
        <template #cell-actions="{ row }">
          <button class="btn-ghost btn-sm" @click="openDetails(row as unknown as AuditLogEntry)">
            Details
          </button>
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

    <SharedAppModal v-model:open="showDetails" title="Audit Entry Details" size="lg">
      <div v-if="selectedAudit" class="space-y-3 text-sm">
        <p><span class="font-medium">Timestamp:</span> {{ formatTs(selectedAudit.createdAt) }}</p>
        <p><span class="font-medium">Actor:</span> {{ selectedAudit.actorFullName ?? "System" }}</p>
        <p><span class="font-medium">Actor Email:</span> {{ selectedAudit.actorEmail ?? "—" }}</p>
        <p><span class="font-medium">Action:</span> {{ actionLabel(selectedAudit.actionType) }}</p>
        <p><span class="font-medium">Entity Type:</span> {{ entityLabel(selectedAudit.entityType) }}</p>
        <p><span class="font-medium">Entity ID:</span> <span class="font-mono text-xs">{{ selectedAudit.entityId ?? "—" }}</span></p>
        <p><span class="font-medium">IP Address:</span> {{ selectedAudit.ipAddress ?? "—" }}</p>
        <div>
          <p class="font-medium mb-1">Payload</p>
          <pre class="rounded-md bg-surface-container-low p-3 text-xs overflow-auto">{{ formattedPayload }}</pre>
        </div>
      </div>
      <template #footer>
        <button class="btn-primary btn-sm" @click="showDetails = false">Close</button>
      </template>
    </SharedAppModal>
  </div>
</template>

<script setup lang="ts">
import type { AuditLogEntry, Page } from "~/types";

definePageMeta({ layout: "default" });

useHead({ title: "Audit Trail — Audita" });

const api = useApi();
const auth = useAuthStore();

if (!(auth.isAdmin || auth.isAuditor)) {
  await navigateTo("/dashboard");
}

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
  { key: "actor", label: "Actor" },
  { key: "actionType", label: "Action" },
  { key: "entityType", label: "Entity Type" },
  { key: "actions", label: "" },
];

// ── State ─────────────────────────────────────────────────────────────────────

const page = ref(1);
const pageSize = 50;
const totalElements = ref(0);
const loadError = ref("");
const exporting = ref(false);
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const filters = reactive({
  rangePreset: "30d",
  actionType: "",
  entityType: "",
  actorEmail: "",
  from: "",
  to: "",
});

const showDetails = ref(false);
const selectedAudit = ref<AuditLogEntry | null>(null);
const canExport = computed(() => auth.isAdmin || auth.isAuditor);

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

watch(
  () => filters.rangePreset,
  () => {
    applyRangePreset();
    page.value = 1;
    refresh();
  },
);

onMounted(() => {
  applyRangePreset();
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

function openDetails(entry: AuditLogEntry) {
  selectedAudit.value = entry;
  showDetails.value = true;
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

function actionLabel(action: string): string {
  const labels: Record<string, string> = {
    CR_CREATED: "Request Created",
    CR_UPDATED: "Request Updated",
    CR_SUBMITTED: "Request Submitted",
    CR_APPROVED: "Request Approved",
    CR_REJECTED: "Request Rejected",
    CR_CANCELLED: "Request Cancelled",
    CR_APPROVER_ADDED: "Approver Added",
    CR_APPROVER_REMOVED: "Approver Removed",
    CR_APPROVERS_REORDERED: "Approvers Reordered",
    CR_ATTACHMENT_UPLOADED: "Attachment Uploaded",
    CR_CUSTOM_FIELDS_UPDATED: "Custom Fields Updated",
    CR_COMMENT_ADDED: "Comment Added",
    SLA_WARNING: "SLA Warning",
    SLA_BREACH: "SLA Breach",
  };
  return labels[action] ?? action;
}

function entityLabel(entityType: string | null): string {
  const labels: Record<string, string> = {
    change_request: "Change Request",
    user: "User",
    group: "Group",
    notification: "Notification",
    settings: "Settings",
  };
  if (!entityType) {
    return "-";
  }
  return labels[entityType] ?? formatToken(entityType);
}

function actorLabel(row: Record<string, unknown>): string {
  const fullName = row.actorFullName as string | null;
  const email = row.actorEmail as string | null;
  return fullName || email || "System";
}

function formatToken(value: string): string {
  return value
    .split(/[_.\s-]+/)
    .map((token) => token.charAt(0).toUpperCase() + token.slice(1).toLowerCase())
    .join(" ");
}

function applyRangePreset() {
  if (filters.rangePreset === "custom") {
    return;
  }
  const now = new Date();
  const to = now.toISOString().slice(0, 10);
  const days = Number(filters.rangePreset.replace("d", ""));
  const fromDate = new Date(now);
  fromDate.setDate(fromDate.getDate() - days);
  filters.from = fromDate.toISOString().slice(0, 10);
  filters.to = to;
}

const formattedPayload = computed(() => {
  if (!selectedAudit.value?.payload) {
    return "{}";
  }
  return JSON.stringify(selectedAudit.value.payload, null, 2);
});

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
