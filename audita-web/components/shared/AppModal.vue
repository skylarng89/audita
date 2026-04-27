<script setup lang="ts">
const props = defineProps<{
  open: boolean;
  title?: string;
  size?: "sm" | "md" | "lg" | "xl";
  closeOnBackdrop?: boolean;
}>();

const emit = defineEmits<{
  close: [];
}>();

function onBackdropClick() {
  if (props.closeOnBackdrop !== false) {
    emit("close");
  }
}

// Trap focus and close on Escape
onMounted(() => {
  function onKeyDown(e: KeyboardEvent) {
    if (e.key === "Escape" && props.open) emit("close");
  }
  window.addEventListener("keydown", onKeyDown);
  onUnmounted(() => window.removeEventListener("keydown", onKeyDown));
});
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        :aria-label="title"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/50" @click="onBackdropClick" />

        <!-- Panel -->
        <Transition
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="opacity-0 scale-95"
          enter-to-class="opacity-100 scale-100"
          leave-active-class="transition duration-150 ease-in"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-95"
        >
          <div
            v-if="open"
            class="relative z-10 w-full bg-white rounded-lg shadow-xl dark:bg-slate-800 flex flex-col max-h-[90vh]"
            :class="{
              'max-w-sm': size === 'sm',
              'max-w-lg': !size || size === 'md',
              'max-w-2xl': size === 'lg',
              'max-w-4xl': size === 'xl',
            }"
          >
            <!-- Header -->
            <div
              class="flex items-center justify-between px-6 py-4 border-b border-border dark:border-border-dark shrink-0"
            >
              <h2
                class="text-base font-semibold text-gray-900 dark:text-gray-100"
              >
                <slot name="title">{{ title }}</slot>
              </h2>
              <button
                type="button"
                class="rounded p-1 text-muted hover:text-gray-700 hover:bg-gray-100 dark:hover:text-gray-200 dark:hover:bg-slate-700 transition-colors"
                aria-label="Close"
                @click="emit('close')"
              >
                <svg
                  class="h-5 w-5"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="px-6 py-5 overflow-y-auto flex-1">
              <slot />
            </div>

            <!-- Footer -->
            <div
              v-if="$slots.footer"
              class="flex items-center justify-end gap-3 px-6 py-4 border-t border-border dark:border-border-dark shrink-0"
            >
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
