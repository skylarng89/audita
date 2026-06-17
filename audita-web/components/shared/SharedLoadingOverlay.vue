<script setup lang="ts">
const { overlayVisible } = useLoadingOverlay();
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      leave-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div
        v-if="overlayVisible"
        class="fixed inset-0 z-[9999] flex items-center justify-center bg-white/80 dark:bg-slate-950/80 backdrop-blur-sm"
      >
        <div class="flex flex-col items-center gap-4">
          <img
            src="/brand/audita-icon-light.svg"
            alt="Audita"
            class="h-10 w-10 dark:hidden"
          />
          <img
            src="/brand/audita-icon-dark.svg"
            alt="Audita"
            class="h-10 w-10 hidden dark:block"
          />
          <div
            class="w-8 h-8 border-3 border-slate-200 dark:border-slate-700 border-t-primary rounded-full animate-spin"
          />
          <p class="text-sm text-muted font-medium">Loading...</p>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
