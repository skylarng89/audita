<template>
  <div class="flex items-center gap-3">
    <CrCompletionStatusBadge :status="completionStatus || 'IN_PROGRESS'" />
    <button
      v-if="canMarkComplete && auth.hasPermission('cr.manage_participants')"
      class="btn-primary btn-sm"
      @click="$emit('completed')"
    >
      Mark Complete
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { CompletionStatus, WorkflowMode } from "~/types";

const props = defineProps<{
  requestId: string;
  completionStatus: CompletionStatus | null;
  approvalStatus: string | null;
  workflowMode: WorkflowMode | null;
  deploymentDone: boolean;
}>();

defineEmits<{
  completed: [];
}>();

const auth = useAuthStore();

const canMarkComplete = computed(() => {
  if (props.completionStatus === "COMPLETED") return false;
  if (props.approvalStatus !== "APPROVED") return false;
  if (props.workflowMode === "APPROVAL_ONLY") return true;
  if (props.workflowMode === "DELIVERY_PIPELINE" && props.deploymentDone) return true;
  return false;
});
</script>
