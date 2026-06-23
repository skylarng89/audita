<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: "primary" | "secondary" | "ghost" | "danger";
    size?: "sm" | "md" | "lg";
    loading?: boolean;
    disabled?: boolean;
    type?: "button" | "submit" | "reset";
  }>(),
  {
    variant: "primary",
    size: "md",
    loading: false,
    disabled: false,
    type: "button",
  },
);
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="[
      'inline-flex items-center justify-center font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
      {
        // Variants
        'bg-primary text-white hover:bg-primary-dark active:bg-primary-dark':
          variant === 'primary',
        'bg-white border border-border text-gray-700 hover:bg-gray-50 dark:bg-surface-dark dark:border-[var(--c-border)] dark:text-gray-200 dark:hover:bg-[var(--c-input)]':
          variant === 'secondary',
        'text-gray-700 hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-[var(--c-input)]':
          variant === 'ghost',
        'bg-danger text-white hover:bg-red-600 active:bg-red-700':
          variant === 'danger',
        // Sizes (include rounding per tier)
        'h-7 px-3 text-xs gap-1.5 rounded-md': size === 'sm',
        'h-9 px-4 text-sm gap-2 rounded-lg': size === 'md',
        'h-11 px-6 text-base gap-2.5 rounded-xl': size === 'lg',
      },
    ]"
  >
    <!-- Loading spinner -->
    <svg
      v-if="loading"
      class="animate-spin shrink-0"
      :class="{
        'h-3 w-3': size === 'sm',
        'h-4 w-4': size === 'md',
        'h-5 w-5': size === 'lg',
      }"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        class="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        stroke-width="4"
      />
      <path
        class="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>

    <slot />
  </button>
</template>
