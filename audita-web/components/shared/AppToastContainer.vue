<template>
  <Teleport to="body">
    <div class="fixed bottom-5 right-5 z-50 flex flex-col gap-2 w-80">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="flex items-start gap-3 rounded-xl shadow-card-md px-4 py-3.5 text-sm font-medium border"
          :class="{
            'bg-white text-on-surface border-outline-variant/50':
              toast.type === 'info',
            'bg-white text-on-surface border-success/30':
              toast.type === 'success',
            'bg-white text-on-surface border-danger/30': toast.type === 'error',
            'bg-white text-on-surface border-warning/30':
              toast.type === 'warning',
          }"
        >
          <!-- Icon -->
          <span class="mt-0.5 shrink-0">
            <span
              v-if="toast.type === 'success'"
              class="w-5 h-5 rounded-full bg-success-light flex items-center justify-center"
            >
              <svg
                class="w-3 h-3 text-success"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="3"
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </span>
            <span
              v-else-if="toast.type === 'error'"
              class="w-5 h-5 rounded-full bg-danger-light flex items-center justify-center"
            >
              <svg
                class="w-3 h-3 text-danger"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="3"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </span>
            <span
              v-else-if="toast.type === 'warning'"
              class="w-5 h-5 rounded-full bg-warning-light flex items-center justify-center"
            >
              <svg
                class="w-3 h-3 text-warning"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"
                />
              </svg>
            </span>
            <span
              v-else
              class="w-5 h-5 rounded-full bg-primary/10 flex items-center justify-center"
            >
              <svg
                class="w-3 h-3 text-primary"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </span>
          </span>
          <span class="flex-1 text-on-surface">{{ toast.message }}</span>
          <button
            @click="dismiss(toast.id)"
            class="text-muted hover:text-on-surface ml-1 shrink-0 transition-colors"
            aria-label="Close"
          >
            <svg
              class="w-4 h-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
const { toasts, dismiss } = useToast();
</script>

<style scoped>
.toast-enter-active {
  transition: all 0.25s ease;
}
.toast-leave-active {
  transition: all 0.2s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(24px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(24px);
}
</style>
