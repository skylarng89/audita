<template>
  <span :class="badgeClass">{{ label }}</span>
</template>

<script setup lang="ts">
import type { CRStatus } from '~/types'

const props = defineProps<{ status: CRStatus }>()

const label = computed(() => ({
  DRAFT: 'Draft',
  PENDING_APPROVAL: 'Pending Approval',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}[props.status] ?? props.status))

const badgeClass = computed(() => ({
  DRAFT: 'badge badge-draft',
  PENDING_APPROVAL: 'badge badge-pending',
  APPROVED: 'badge badge-approved',
  REJECTED: 'badge badge-rejected',
  CANCELLED: 'badge badge-cancelled',
}[props.status] ?? 'badge badge-draft'))
</script>
