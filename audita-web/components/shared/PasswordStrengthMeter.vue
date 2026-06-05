<template>
  <div class="flex gap-1 mt-1.5" aria-hidden="true">
    <div
      v-for="i in 4"
      :key="i"
      class="h-1 flex-1 rounded-full transition-colors"
      :class="barClass(i)"
    />
  </div>
  <p class="text-xs text-muted mt-1" aria-live="polite" aria-atomic="true">
    Password strength: {{ label }}
  </p>
</template>

<script setup lang="ts">
const props = defineProps<{ password: string }>()

const score = computed(() => {
  const p = props.password
  if (!p) return 0
  let s = 0
  if (p.length >= 8) s++
  if (p.length >= 12) s++
  if (/[A-Z]/.test(p)) s++
  if (/[a-z]/.test(p)) s++
  if (/\d/.test(p)) s++
  if (/[^A-Za-z0-9]/.test(p)) s++
  return Math.min(4, s)
})

const label = computed(() => {
  const labels = ["", "Weak", "Fair", "Good", "Strong"]
  return labels[score.value] ?? ""
})

function barClass(bar: number) {
  if (bar > score.value) return "bg-border dark:bg-border-dark"
  if (score.value <= 1) return "bg-danger"
  if (score.value === 2) return "bg-warning"
  if (score.value === 3) return "bg-info"
  return "bg-success"
}
</script>
