<template>
  <span :class="badgeClass">{{ props.priority }}</span>
</template>

<script setup lang="ts">
import type { Priority } from '~/types'

const props = defineProps<{ priority: Priority }>()

const badgeClass = computed(() => ({
  LOW: 'badge badge-low',
  MEDIUM: 'badge badge-medium',
  HIGH: 'badge badge-high',
  CRITICAL: 'badge badge-critical',
}[props.priority] ?? 'badge'))
</script>
