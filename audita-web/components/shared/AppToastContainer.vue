<template>
  <Teleport to="body">
    <div
      role="log"
      aria-live="polite"
      aria-atomic="false"
      class="fixed bottom-5 right-5 z-50 flex flex-col gap-2 w-80"
    >
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="relative flex items-start gap-3 rounded-xl shadow-card-md px-4 py-3.5 text-sm font-medium border overflow-hidden"
          :class="{
            'bg-white text-on-surface border-outline-variant/50 dark:bg-blue-950 dark:text-blue-300 dark:border-blue-800':
              toast.type === 'info',
            'bg-white text-on-surface border-success/30 dark:bg-green-950 dark:text-green-300 dark:border-green-800':
              toast.type === 'success',
            'bg-white text-on-surface border-danger/30 dark:bg-red-950 dark:text-red-300 dark:border-red-800': toast.type === 'error',
            'bg-white text-on-surface border-warning/30 dark:bg-amber-950 dark:text-amber-300 dark:border-amber-800':
              toast.type === 'warning',
          }"
          :role="toast.type === 'error' ? 'alert' : undefined"
        >
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
            aria-label="Close notification"
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

          <div class="absolute bottom-0 left-0 right-0 h-0.5">
            <div
              class="h-full origin-left toast-progress"
              :class="{
                'bg-primary/30': toast.type === 'info',
                'bg-success/40': toast.type === 'success',
                'bg-danger/40': toast.type === 'error',
                'bg-warning/40': toast.type === 'warning',
              }"
              :style="`animation-duration: ${toast.duration}ms`"
            />
          </div>
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

@keyframes toast-drain {
  from {
    transform: scaleX(1);
  }
  to {
    transform: scaleX(0);
  }
}

.toast-progress {
  animation: toast-drain linear forwards;
}
</style>
