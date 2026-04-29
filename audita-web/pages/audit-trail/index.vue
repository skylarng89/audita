<template>
  <div class="space-y-6">
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
      <button type="button" class="btn-secondary btn-sm">Export</button>
    </div>

    <section class="card p-5 shadow-card-hover">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-4">
        <div>
          <label class="field-label" for="audit-event-type">Event Type</label>
          <select
            id="audit-event-type"
            v-model="filters.eventType"
            class="input"
          >
            <option value="">All Events</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="audit-actor">Actor</label>
          <input
            id="audit-actor"
            v-model="filters.actor"
            type="text"
            class="input"
            placeholder="Search actor"
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
definePageMeta({ layout: "default" });

const api = useApi();

interface AuditRow {
  id: string;
  timestamp: string;
  actor: string;
  action: string;
  target: string;
  severity?: string;
}

interface AuditPayload {
  content?: AuditRow[];
}

const filters = reactive({
  eventType: "",
  actor: "",
  from: "",
  to: "",
});

const loadError = ref("");

const { data, pending } = await useAsyncData<AuditPayload>(
  "audit-trail-page",
  async () => {
    loadError.value = "";

    try {
      return (await api("/api/v1/audit-trail")) as AuditPayload;
    } catch {
      loadError.value = "No data returned.";
      return { content: [] };
    }
  },
);

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
  { key: "timestamp", label: "Timestamp" },
  { key: "actor", label: "Actor" },
  { key: "action", label: "Action" },
  { key: "target", label: "Target" },
  { key: "severity", label: "Severity" },
];
</script>
