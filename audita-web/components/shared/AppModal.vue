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

let modalIdSequence = 0;

// Unique id for aria-labelledby association
function createSecureId() {
  const cryptoApi = globalThis.crypto;
  if (cryptoApi?.getRandomValues) {
    const values = new Uint32Array(1);
    cryptoApi.getRandomValues(values);
    return "modal-title-" + values[0].toString(36);
  }
  modalIdSequence += 1;
  return "modal-title-" + modalIdSequence.toString(36);
}

const titleId = createSecureId();
const panelRef = ref<HTMLElement | null>(null);

function onBackdropClick() {
  if (props.closeOnBackdrop !== false) {
    emit("close");
  }
}

const FOCUSABLE_SELECTOR = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  '[tabindex]:not([tabindex="-1"])',
].join(",");

function getFocusable(): HTMLElement[] {
  if (!panelRef.value) return [];
  return Array.from(
    panelRef.value.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR),
  );
}

function onKeyDown(e: KeyboardEvent) {
  if (!props.open) return;
  if (e.key === "Escape") {
    emit("close");
    return;
  }
  if (e.key !== "Tab") return;
  const focusable = getFocusable();
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault();
    last.focus();
    return;
  }
  if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault();
    first.focus();
  }
}

// Auto-focus the first interactive element when modal opens
watch(
  () => props.open,
  (open) => {
    if (open) {
      nextTick(() => {
        getFocusable()[0]?.focus();
      });
    }
  },
);

onMounted(() => {
  globalThis.addEventListener("keydown", onKeyDown);
  onUnmounted(() => globalThis.removeEventListener("keydown", onKeyDown));
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
      <dialog
        v-if="open"
        open
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        aria-modal="true"
        :aria-labelledby="titleId"
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
            ref="panelRef"
            class="relative z-10 w-full bg-white rounded-2xl shadow-[0_20px_40px_rgba(0,35,111,0.12)] dark:bg-slate-800 flex flex-col max-h-[90vh]"
            :class="{
              'max-w-sm': size === 'sm',
              'max-w-lg': !size || size === 'md',
              'max-w-2xl': size === 'lg',
              'max-w-4xl': size === 'xl',
            }"
          >
            <!-- Header -->
            <div
              class="flex items-center justify-between px-6 py-4 border-b border-outline-variant/50 dark:border-border-dark shrink-0"
            >
              <h2
                :id="titleId"
                class="text-base font-semibold text-on-surface dark:text-gray-100"
              >
                <slot name="title">{{ title }}</slot>
              </h2>
              <button
                type="button"
                class="rounded-lg p-1 text-muted hover:text-on-surface hover:bg-surface-container dark:hover:text-gray-200 dark:hover:bg-slate-700 transition-colors"
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
              class="flex items-center justify-end gap-3 px-6 py-4 border-t border-outline-variant/50 dark:border-border-dark shrink-0 bg-surface-container-low dark:bg-slate-800/50 rounded-b-2xl"
            >
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </dialog>
    </Transition>
  </Teleport>
</template>
