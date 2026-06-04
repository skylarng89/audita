<template>
  <span :class="badgeClass">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CompletionStatus } from '~/types'

const props = defineProps<{ status: CompletionStatus }>()

const label = computed(() => ({
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
}[props.status] ?? props.status))

const badgeClass = computed(() => ({
  IN_PROGRESS: 'badge badge-pending',
  COMPLETED: 'badge badge-approved',
}[props.status] ?? 'badge badge-draft'))
</script>
