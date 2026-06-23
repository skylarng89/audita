<script setup lang="ts">
defineProps<{
  variant?: "success" | "warning" | "danger" | "info" | "neutral" | "primary";
  size?: "sm" | "md";
  dot?: boolean;
}>();
</script>

<template>
  <span
    :class="[
      'inline-flex items-center gap-1.5 font-medium rounded-full',
      {
        'text-xs px-2 py-0.5': !$props.size || $props.size === 'sm',
        'text-sm px-2.5 py-1': $props.size === 'md',
        // Colour variants
        'bg-success-light text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400':
          variant === 'success',
        'bg-warning-light text-amber-800 dark:bg-amber-900/30 dark:text-amber-400':
          variant === 'warning',
        'bg-danger-light text-red-800 dark:bg-red-900/30 dark:text-red-400':
          variant === 'danger',
        'bg-info-light text-blue-800 dark:bg-blue-900/30 dark:text-blue-400':
          variant === 'info',
        'bg-gray-100 text-gray-700 dark:bg-[var(--c-input)] dark:text-gray-300':
          variant === 'neutral' || !variant,
        'bg-primary/10 text-primary dark:bg-primary/20 dark:text-blue-300':
          variant === 'primary',
      },
    ]"
  >
    <!-- Status dot -->
    <span
      v-if="dot"
      class="h-1.5 w-1.5 rounded-full shrink-0"
      :class="{
        'bg-success': variant === 'success',
        'bg-warning': variant === 'warning',
        'bg-danger': variant === 'danger',
        'bg-info': variant === 'info',
        'bg-muted': variant === 'neutral' || !variant,
        'bg-primary': variant === 'primary',
      }"
    />
    <slot />
  </span>
</template>
