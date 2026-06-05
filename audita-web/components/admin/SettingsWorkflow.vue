<template>
  <section class="card p-5 shadow-card-hover">
    <h2 class="text-lg font-semibold">Workflow Defaults</h2>
    <div class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
      <div>
        <label class="field-label" for="approval-type-default"
          >Default Approval Type</label
        >
        <select
          id="approval-type-default"
          :model-value="modelValue.approvalTypeDefault"
          class="input"
          @change="$emit('update:approvalTypeDefault', ($event.target as HTMLSelectElement).value)"
        >
          <option value="LINEAR">Linear</option>
          <option value="NON_LINEAR">Non-linear</option>
        </select>
      </div>
      <div class="rounded-lg border border-border/70 bg-surface-container-low p-3 text-sm text-muted dark:border-border-dark dark:bg-slate-900/50">
        Configured default approver users and groups are always auto-added to
        new change requests.
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: { approvalTypeDefault: string; requireDefaultApprovers: boolean }
}>()

defineEmits<{
  "update:approvalTypeDefault": [value: string]
}>()
</script>
