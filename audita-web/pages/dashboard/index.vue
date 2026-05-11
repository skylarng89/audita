<template>
  <div class="space-y-6">
    <div>
      <p
        class="text-[11px] font-semibold uppercase tracking-[0.16em] text-primary/60 mb-1"
      >
        Operations Overview
      </p>
      <h1 class="text-3xl font-bold tracking-tight text-on-surface">
        Audita Dashboard
      </h1>
      <p class="text-sm text-muted mt-1">
        Overview of your organisation's change activity.
      </p>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="card p-5 shadow-card-hover">
        <div class="flex items-start justify-between">
          <p
            class="text-[11px] font-semibold uppercase tracking-[0.12em] text-muted"
          >
            Pending Approvals
          </p>
          <div
            class="w-7 h-7 rounded-lg bg-primary/10 flex items-center justify-center"
          >
            <svg
              class="w-3.5 h-3.5 text-primary"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
        </div>
        <p class="text-3xl font-bold text-on-surface mt-2">
          {{ stats.pending }}
        </p>
        <p class="text-xs text-muted mt-1">Live pending approvals</p>
      </div>
      <div class="card p-5 shadow-card-hover">
        <div class="flex items-start justify-between">
          <p
            class="text-[11px] font-semibold uppercase tracking-[0.12em] text-muted"
          >
            Active Changes
          </p>
          <div
            class="w-7 h-7 rounded-lg bg-secondary/10 flex items-center justify-center"
          >
            <svg
              class="w-3.5 h-3.5 text-secondary"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M13 10V3L4 14h7v7l9-11h-7z"
              />
            </svg>
          </div>
        </div>
        <p class="text-3xl font-bold text-on-surface mt-2">
          {{ stats.active }}
        </p>
        <p class="text-xs text-muted mt-1">Currently in progress</p>
      </div>
      <div class="card p-5 border-danger/20 shadow-card-hover bg-danger/[0.02]">
        <div class="flex items-start justify-between">
          <p
            class="text-[11px] font-semibold uppercase tracking-[0.12em] text-danger/80"
          >
            SLA Risks
          </p>
          <div
            class="w-7 h-7 rounded-lg bg-danger/10 flex items-center justify-center"
          >
            <svg
              class="w-3.5 h-3.5 text-danger"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
          </div>
        </div>
        <p class="text-3xl font-bold text-danger mt-2">{{ stats.slaRisks }}</p>
        <p class="text-xs text-danger/70 mt-1">Critical response needed</p>
      </div>
      <div
        class="card p-5 bg-primary text-white border-0 shadow-[0_4px_12px_rgba(0,35,111,0.3)]"
      >
        <div class="flex items-start justify-between">
          <p
            class="text-[11px] font-semibold uppercase tracking-[0.12em] text-white/60"
          >
            Success Rate
          </p>
          <div
            class="w-7 h-7 rounded-lg bg-white/15 flex items-center justify-center"
          >
            <svg
              class="w-3.5 h-3.5 text-white"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
        </div>
        <p class="text-3xl font-bold mt-2">{{ stats.successRate }}%</p>
        <p class="text-xs text-white/50 mt-1">Closed requests approved</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2">
        <div class="flex items-center justify-between mb-3">
          <h2 class="font-semibold text-on-surface">
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
              <button
                class="btn-secondary btn-sm"
                :aria-label="`Decline change request: ${cr.title}`"
                @click="rejectCr(cr.id)"
              >
                Decline
              </button>
              <button
                class="btn-primary btn-sm"
                :aria-label="`Approve change request: ${cr.title}`"
                @click="approveCr(cr.id)"
              >
                Approve
              </button>
            </div>
          </div>
        </div>
      </div>

      <div>
        <h2 class="font-semibold text-on-surface mb-3">Recent Activity</h2>
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
              <p class="text-sm text-on-surface leading-snug">
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
import type { ChangeRequest, Notification } from "~/types";
import { formatDistanceToNow, parseISO } from "date-fns";

definePageMeta({ middleware: "auth" });

useHead({ title: "Dashboard — Audita" });

const api = useApi();
const auth = useAuthStore();
const { list, approve } = useChangeRequests();
const { error: toastError } = useToast();

interface DashboardSummaryResponse {
  pendingApprovals: number;
  activeChanges: number;
  slaRisks: number;
  successRate: number;
}

const stats = reactive({
  pending: 0,
  active: 0,
  slaRisks: 0,
  successRate: 0,
});
const pendingCRs = ref<ChangeRequest[]>([]);
const recentActivity = ref<Notification[]>([]);

async function load() {
  try {
    const [summary, pendingPage, notifications] = await Promise.all([
      api<DashboardSummaryResponse>("/api/v1/dashboard/summary", {
        method: "GET",
      }),
      list({ status: "PENDING_APPROVAL", size: 3 }),
      api<Notification[]>("/api/v1/notifications", {
        method: "GET",
        query: { page: 0, size: 5 },
      }),
    ]);

    stats.pending = summary.pendingApprovals;
    stats.active = summary.activeChanges;
    stats.slaRisks = summary.slaRisks;
    stats.successRate = Number(summary.successRate.toFixed(1));

    pendingCRs.value = pendingPage.content;
    recentActivity.value = notifications;
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

function formatActivity(entry: Notification) {
  if (entry.title && entry.title.trim().length > 0) {
    return entry.title;
  }
  return entry.body ?? "Notification";
}

function formatDate(iso: string) {
  return formatDistanceToNow(parseISO(iso), { addSuffix: true });
}

onMounted(load);
</script>
