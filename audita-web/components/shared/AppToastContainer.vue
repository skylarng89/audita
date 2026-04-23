<template>
  <Teleport to="body">
    <div class="fixed bottom-5 right-5 z-50 flex flex-col gap-2 w-72">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="flex items-start gap-3 rounded-lg shadow-lg px-4 py-3 text-sm font-medium text-white"
          :class="{
            'bg-success': toast.type === 'success',
            'bg-danger':  toast.type === 'error',
            'bg-primary': toast.type === 'info',
            'bg-warning text-gray-900': toast.type === 'warning',
          }"
        >
          <span class="flex-1">{{ toast.message }}</span>
          <button
            @click="dismiss(toast.id)"
            class="opacity-70 hover:opacity-100 ml-2 shrink-0"
            aria-label="Close"
          >&times;</button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
const { toasts, dismiss } = useToast()
</script>

<style scoped>
.toast-enter-active  { transition: all 0.25s ease; }
.toast-leave-active  { transition: all 0.2s ease; }
.toast-enter-from    { opacity: 0; transform: translateX(24px); }
.toast-leave-to      { opacity: 0; transform: translateX(24px); }
</style>
