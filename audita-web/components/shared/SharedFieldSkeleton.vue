<template>
  <div
    :class="[
      heightClass,
      rounded ? 'rounded-lg' : 'rounded',
      'animate-pulse bg-slate-200 dark:bg-[var(--c-input)]',
    ]"
  />
</template>

<script setup lang="ts">
defineProps<{
  heightClass: string;
  rounded?: boolean;
}>();
</script>
