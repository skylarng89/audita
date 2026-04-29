<template>
  <div class="space-y-6">
    <div>
      <p
        class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
      >
        Operations Overview
      </p>
      <h1
        class="text-3xl font-bold tracking-tight text-gray-900 dark:text-gray-100"
      >
        Audita Dashboard
      </h1>
      <p class="text-sm text-muted mt-1">
        Overview of your organisation's change activity.
      </p>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="card p-4 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">
          Pending Approvals
        </p>
        <p class="text-3xl font-bold text-gray-900 dark:text-gray-100 mt-1">
          {{ stats.pending }}
        </p>
        <p class="text-xs text-muted mt-1">+2 since yesterday</p>
      </div>
      <div class="card p-4 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">
          Active Changes
        </p>
        <p class="text-3xl font-bold text-gray-900 dark:text-gray-100 mt-1">
          {{ stats.active }}
        </p>
      </div>
      <div class="card p-4 border-danger/30 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-danger">
          SLA Risks
        </p>
        <p class="text-3xl font-bold text-danger mt-1">{{ stats.slaRisks }}</p>
        <p class="text-xs text-danger/70 mt-1">Critical response needed</p>
      </div>
      <div class="card p-4 bg-primary text-white border-0 shadow-card-hover">
        <p class="text-xs font-semibold uppercase tracking-wide text-white/70">
          Success Rate
        </p>
        <p class="text-3xl font-bold mt-1">{{ stats.successRate }}%</p>
        <p class="text-xs text-white/60 mt-1">Global KPI</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2">
        <div class="flex items-center justify-between mb-3">
          <h2 class="font-semibold text-gray-900 dark:text-gray-100">
            Changes Awaiting My Action
          </h2>
          <NuxtLink
            to="/change-requests?status=PENDING_APPROVAL"
            class="text-xs text-primary hover:underline font-medium"
          >
            View All
          </NuxtLink>
        </div>

        <div class="space-y-3">
          <div
            v-if="pendingCRs.length === 0"
            class="card p-8 text-center text-sm text-muted"
          >
            No change requests awaiting your action.
          </div>

          <div
            v-for="cr in pendingCRs"
            :key="cr.id"
            class="card p-4 flex items-center gap-4 shadow-card-hover"
          >
            <div
              class="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center shrink-0"
            >
              <svg
                class="w-4 h-4 text-primary"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                />
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-semibold text-sm truncate">{{ cr.title }}</p>
              <p class="text-xs text-muted mt-0.5">
                {{ cr.id.slice(0, 12) }} &bull; Risk:
                <span
                  :class="riskClass(cr.riskLevel)"
                  class="font-medium uppercase"
                  >{{ cr.riskLevel }}</span
                >
              </p>
            </div>
            <div v-if="auth.canApprove" class="flex gap-2 shrink-0">
              <button @click="rejectCr(cr.id)" class="btn-secondary btn-sm">
                Decline
              </button>
              <button @click="approveCr(cr.id)" class="btn-primary btn-sm">
                Approve
              </button>
            </div>
          </div>
        </div>
      </div>

      <div>
        <h2 class="font-semibold text-gray-900 dark:text-gray-100 mb-3">
          Recent Activity
        </h2>
        <div class="card p-4 space-y-4 shadow-card-hover">
          <div
            v-if="recentActivity.length === 0"
            class="text-sm text-muted text-center py-4"
          >
            No recent activity.
          </div>
          <div
            v-for="entry in recentActivity"
            :key="entry.id"
            class="flex gap-3"
          >
            <div class="w-2 h-2 rounded-full bg-primary mt-1.5 shrink-0" />
            <div class="min-w-0">
              <p class="text-sm text-gray-700 dark:text-gray-300 leading-snug">
                {{ formatActivity(entry) }}
              </p>
              <p class="text-xs text-muted mt-0.5">
                {{ formatDate(entry.createdAt) }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChangeRequest, ActivityEntry } from "~/types";
import { formatDistanceToNow, parseISO } from "date-fns";

definePageMeta({ middleware: "auth" });

const auth = useAuthStore();
const { list, approve, listActivity } = useChangeRequests();
const { error: toastError } = useToast();

const stats = reactive({
  pending: 0,
  active: 0,
  slaRisks: 0,
  successRate: 99.2,
});
const pendingCRs = ref<ChangeRequest[]>([]);
const recentActivity = ref<ActivityEntry[]>([]);

async function load() {
  try {
    const [pendingPage] = await Promise.all([
      list({ status: "PENDING_APPROVAL", size: 3 }),
    ]);
    pendingCRs.value = pendingPage.content;
    stats.pending = pendingPage.totalElements;
  } catch {
    toastError("Failed to load dashboard data.");
  }
}

async function approveCr(id: string) {
  try {
    await approve(id);
    await load();
  } catch {
    toastError("Failed to approve change request.");
  }
}

function rejectCr(id: string) {
  navigateTo(`/change-requests/${id}#reject`);
}

function riskClass(level: string) {
  const map: Record<string, string> = {
    LOW: "text-muted",
    MEDIUM: "text-info",
    HIGH: "text-warning",
    CRITICAL: "text-danger",
  };
  return map[level] ?? "text-muted";
}

function formatActivity(entry: ActivityEntry) {
  return `${entry.actor?.fullName ?? "System"} — ${entry.actionType.replace(/_/g, " ").toLowerCase()}`;
}

function formatDate(iso: string) {
  return formatDistanceToNow(parseISO(iso), { addSuffix: true });
}

onMounted(load);
</script>
