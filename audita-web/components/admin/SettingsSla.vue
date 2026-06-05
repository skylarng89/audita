<template>
  <section class="card p-5 shadow-card-hover">
    <h2 class="text-lg font-semibold">SLA Defaults</h2>
    <p class="text-sm text-muted mt-1">
      Configure default SLA deadlines per priority and warning threshold.
    </p>
    <div class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
      <div>
        <label class="field-label" for="sla-low">Low (hours)</label>
        <input
          id="sla-low"
          :model-value="modelValue.lowHours"
          type="number"
          min="1"
          max="720"
          class="input"
          @input="$emit('update:lowHours', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div>
        <label class="field-label" for="sla-medium">Medium (hours)</label>
        <input
          id="sla-medium"
          :model-value="modelValue.mediumHours"
          type="number"
          min="1"
          max="720"
          class="input"
          @input="$emit('update:mediumHours', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div>
        <label class="field-label" for="sla-high">High (hours)</label>
        <input
          id="sla-high"
          :model-value="modelValue.highHours"
          type="number"
          min="1"
          max="720"
          class="input"
          @input="$emit('update:highHours', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div>
        <label class="field-label" for="sla-critical">Critical (hours)</label>
        <input
          id="sla-critical"
          :model-value="modelValue.criticalHours"
          type="number"
          min="1"
          max="720"
          class="input"
          @input="$emit('update:criticalHours', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div>
        <label class="field-label" for="sla-warning"
          >Warning Before (hours)</label
        >
        <input
          id="sla-warning"
          :model-value="modelValue.warningBeforeHours"
          type="number"
          min="1"
          max="168"
          class="input"
          @input="$emit('update:warningBeforeHours', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: {
    lowHours: number
    mediumHours: number
    highHours: number
    criticalHours: number
    warningBeforeHours: number
  }
}>()

defineEmits<{
  "update:lowHours": [value: number]
  "update:mediumHours": [value: number]
  "update:highHours": [value: number]
  "update:criticalHours": [value: number]
  "update:warningBeforeHours": [value: number]
}>()
</script>
