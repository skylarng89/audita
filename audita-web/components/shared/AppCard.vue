<script setup lang="ts">
defineProps<{
  padding?: boolean;
  noBorder?: boolean;
}>();
</script>

<template>
  <div
    :class="[
      'bg-white rounded-lg shadow-card dark:bg-[var(--c-surface)]',
      !noBorder && 'border border-border dark:border-[var(--c-border)]',
    ]"
  >
    <!-- Header -->
    <div
      v-if="$slots.header"
      class="flex items-center justify-between px-5 py-4 border-b border-border dark:border-[var(--c-border)]"
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
      class="flex items-center justify-end gap-3 px-5 py-4 border-t border-border dark:border-[var(--c-border)]"
    >
      <slot name="footer" />
    </div>
  </div>
</template>
