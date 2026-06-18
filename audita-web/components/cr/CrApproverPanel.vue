<template>
  <section class="card p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="font-semibold">Approvers</h2>
      <button
        class="btn-ghost btn-md"
        :disabled="!canManageApprovers"
        :title="approverManagementDisabledReason"
        @click="$emit('toggleAdd')"
      >
        Add Approver
      </button>
    </div>

    <slot name="add-section" />

    <TransitionGroup
      name="approver-list"
      tag="div"
      class="space-y-2"
    >
      <div
        v-for="a in sortedApprovers"
        :key="a.id"
        class="border border-border dark:border-border-dark rounded-lg p-3 flex items-center justify-between gap-3 transition-all duration-300 ease-out"
        :class="draggingId === a.id ? 'opacity-60 scale-[0.98]' : ''"
        :draggable="canManageApprovers"
        @dragstart="$emit('dragStart', a.id)"
        @dragend="$emit('dragEnd')"
        @dragover.prevent
        @drop.prevent="$emit('drop', a.id)"
      >
        <div class="flex items-start gap-3">
          <span
            v-if="canManageApprovers"
            class="mt-0.5 text-muted cursor-grab active:cursor-grabbing transition-colors hover:text-on-surface"
            title="Drag to reorder"
            aria-hidden="true"
          >::</span>
          <p class="text-sm font-semibold">
            {{ a.userFullName }}
            <span class="text-xs text-muted">({{ a.userEmail }})</span>
          </p>
          <p class="text-xs text-muted">
            Position {{ a.position }} • {{ a.status }}
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button
            v-if="canManageApprovers && a.status === 'PENDING'"
            class="btn-ghost btn-sm"
            @click="$emit('demote', a.id)"
          >
            Demote to Watcher
          </button>
          <button
            class="btn-ghost btn-md"
            :title="`Move ${a.userFullName} up`"
            @click="$emit('moveUp', a.id)"
            :disabled="!canManageApprovers || a.position === 1"
          >↑</button>
          <button
            class="btn-ghost btn-md"
            :title="`Move ${a.userFullName} down`"
            @click="$emit('moveDown', a.id)"
            :disabled="!canManageApprovers || a.position === sortedApprovers.length"
          >↓</button>
          <button
            class="btn-ghost btn-md"
            :disabled="!canRemove(a)"
            :title="removeReason(a)"
            @click="$emit('remove', a.id)"
          >Remove</button>
        </div>
      </div>
    </TransitionGroup>

    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="translate-y-2 opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="translate-y-0 opacity-100"
      leave-to-class="translate-y-2 opacity-0"
    >
      <div
        v-if="hasChanges && canManageApprovers"
        class="mt-4 rounded-lg border border-primary/30 bg-primary/5 dark:bg-primary/10 p-3 flex items-center justify-between gap-3"
      >
        <p class="text-sm text-primary font-medium">
          Approver changes applied
        </p>
        <button class="btn-ghost btn-sm" @click="$emit('snapshot')">
          Done
        </button>
      </div>
    </Transition>
  </section>
</template>

<script setup lang="ts">
import type { CrApprover } from "~/types"

defineProps<{
  sortedApprovers: CrApprover[]
  draggingId: string | null
  canManageApprovers: boolean
  approverManagementDisabledReason?: string
  hasChanges: boolean
  canRemove: (a: CrApprover) => boolean
  removeReason: (a: CrApprover) => string | undefined
}>()

defineEmits<{
  toggleAdd: []
  dragStart: [id: string]
  dragEnd: []
  drop: [id: string]
  demote: [id: string]
  moveUp: [id: string]
  moveDown: [id: string]
  remove: [id: string]
  snapshot: []
}>()
</script>
