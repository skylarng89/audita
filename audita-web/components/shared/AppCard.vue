<script setup lang="ts">
defineProps<{
  padding?: boolean;
  noBorder?: boolean;
}>();
</script>

<template>
  <div
    :class="[
      'bg-white rounded-lg shadow-card dark:bg-slate-800',
      !noBorder && 'border border-border dark:border-border-dark',
    ]"
  >
    <!-- Header -->
    <div
      v-if="$slots.header"
      class="flex items-center justify-between px-5 py-4 border-b border-border dark:border-border-dark"
    >
      <slot name="header" />
    </div>

    <!-- Body -->
    <div :class="padding !== false ? 'p-5' : ''">
      <slot />
    </div>

    <!-- Footer -->
    <div
      v-if="$slots.footer"
      class="flex items-center justify-end gap-3 px-5 py-4 border-t border-border dark:border-border-dark"
    >
      <slot name="footer" />
    </div>
  </div>
</template>
