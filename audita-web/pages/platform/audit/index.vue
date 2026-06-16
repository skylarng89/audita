<template>
  <div class="space-y-6">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Platform Governance
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Platform Audit Logs</h1>
        <p class="text-sm text-muted mt-1">
          Observe tenant provisioning, suspension, and privileged platform
          events.
        </p>
      </div>
      <button type="button" class="btn-secondary btn-sm">Export</button>
    </div>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
      <section class="card p-5 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">
          Events in View
        </p>
        <p class="mt-2 text-3xl font-bold">{{ rows.length }}</p>
      </section>
      <section class="card p-5 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">
          Tenant Scope
        </p>
        <p class="mt-2 text-sm text-muted">
          {{ selectedTenant || "All tenants" }}
        </p>
      </section>
      <section class="card p-5 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">
          Signal
        </p>
        <p class="mt-2 text-sm text-muted">
          {{ loadError ? "Unavailable" : "Healthy" }}
        </p>
      </section>
    </div>

    <section class="card p-5 shadow-card-hover">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-3">
        <div>
          <label class="field-label" for="platform-audit-tenant">Tenant</label>
          <select
            id="platform-audit-tenant"
            v-model="selectedTenant"
            class="input"
          >
            <option value="">All tenants</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="platform-audit-action">Action</label>
          <select
            id="platform-audit-action"
            v-model="selectedAction"
            class="input"
          >
            <option value="">All actions</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="platform-audit-date">Date</label>
          <input
            id="platform-audit-date"
            v-model="selectedDate"
            type="date"
            class="input"
          />
        </div>
      </div>
    </section>

    <section class="card shadow-card-hover">
      <AppTable
        :columns="columns"
        :data="rows"
        :loading="pending"
        row-key="id"
        empty-message="No data returned."
      >
        <template #cell-severity="{ value }">
          <AppBadge :variant="severityVariant(value)">{{
            value || "N/A"
          }}</AppBadge>
        </template>
      </AppTable>
    </section>

    <p v-if="loadError" class="text-sm text-danger">{{ loadError }}</p>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "platform" });

import { useLoadingOverlay } from "~/composables/useLoadingOverlay";

const api = useApi();

interface PlatformAuditRow {
  id: string;
  occurredAt: string;
  tenantName: string;
  action: string;
  actor: string;
  severity?: string;
}

interface PlatformAuditPayload {
  content?: PlatformAuditRow[];
}

const selectedTenant = ref("");
const selectedAction = ref("");
const selectedDate = ref("");
const loadError = ref("");

const { data, pending } = await useAsyncData<PlatformAuditPayload>(
  "platform-audit-page",
  async () => {
    loadError.value = "";

    try {
      return (await api("/api/platform/v1/audit")) as PlatformAuditPayload;
    } catch {
      loadError.value = "No data returned.";
      return { content: [] };
    }
  },
);

const { hide: hideLoading } = useLoadingOverlay();
watch(pending, (val) => { if (!val) hideLoading(); });

const rows = computed(() => data.value?.content ?? []);

function severityVariant(severity: unknown): "danger" | "warning" | "neutral" {
  if (severity === "HIGH" || severity === "CRITICAL") {
    return "danger";
  }

  if (severity === "MEDIUM") {
    return "warning";
  }

  return "neutral";
}

const columns = [
  { key: "occurredAt", label: "Timestamp" },
  { key: "tenantName", label: "Tenant" },
  { key: "action", label: "Action" },
  { key: "actor", label: "Actor" },
  { key: "severity", label: "Severity" },
];
</script>
