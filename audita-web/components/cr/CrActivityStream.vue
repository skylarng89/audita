<template>
  <section class="card p-5">
    <h2 class="font-semibold mb-2">Activity Stream</h2>
    <div v-if="!activity.length" class="text-sm text-muted">
      No activity yet.
    </div>
    <div v-else class="space-y-3">
      <div
        v-for="event in activity"
        :key="event.id"
        class="border border-border dark:border-[var(--c-border)] rounded-xl p-4 bg-surface-container-low/50 dark:bg-[var(--c-surface)]/70"
      >
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p class="text-sm font-semibold text-on-surface dark:text-gray-100">
              {{ formatActivityAction(event.actionType) }}
            </p>
            <p class="text-xs text-muted mt-1">
              {{ event.actorFullName ?? "System" }} •
              {{ fmt(event.createdAt) }}
            </p>
          </div>
          <span class="badge badge-draft">{{
            formatActivityBadge(event.actionType)
          }}</span>
        </div>
        <p
          v-if="summary(event)"
          class="mt-3 text-sm text-gray-700 dark:text-gray-300"
        >
          {{ summary(event) }}
        </p>
        <dl
          v-if="fields(event).length"
          class="mt-3 grid gap-2 rounded-lg bg-white/80 p-3 text-xs dark:bg-[var(--c-surface)]/50 sm:grid-cols-2"
        >
          <div v-for="f in fields(event)" :key="f.label">
            <dt class="font-semibold uppercase tracking-[0.08em] text-muted">
              {{ f.label }}
            </dt>
            <dd class="mt-1 text-sm text-on-surface dark:text-gray-200">
              {{ f.value }}
            </dd>
          </div>
        </dl>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ActivityEntry } from "~/types"
import {
  buildActivitySummary,
  formatActivityAction,
  formatActivityBadge,
  activityFields,
} from "~/composables/activitySummary"

defineProps<{
  activity: ActivityEntry[]
  fmt: (value: string) => string
}>()

function summary(event: ActivityEntry) {
  return buildActivitySummary(event)
}

function fields(event: ActivityEntry) {
  return activityFields(event)
}
</script>
