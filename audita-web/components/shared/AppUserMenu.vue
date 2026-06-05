<template>
  <div class="relative" ref="menuRef">
    <button
      @click="open = !open"
      class="flex items-center gap-2 rounded-lg hover:bg-surface-container dark:hover:bg-slate-800 px-2 py-1.5 transition-colors"
    >
      <div class="text-right hidden sm:block">
        <p
          class="text-xs font-semibold leading-none text-on-surface dark:text-gray-100"
        >
          {{ auth.fullName }}
        </p>
        <p class="text-[10px] text-muted mt-0.5 uppercase tracking-wide">
          {{ auth.role }}
        </p>
      </div>
      <div
        class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white text-xs font-bold uppercase shadow-sm"
      >
        {{ initials }}
      </div>
    </button>

    <Transition name="fade">
      <div
        v-if="open"
        class="absolute right-0 mt-2 w-52 bg-white dark:bg-slate-800 shadow-card-md rounded-xl overflow-hidden z-50 py-1 border border-outline-variant/50 dark:border-border-dark"
      >
        <div
          class="px-4 py-3 border-b border-outline-variant/40 dark:border-border-dark"
        >
          <p class="text-sm font-semibold text-on-surface">
            {{ auth.fullName }}
          </p>
          <p class="text-xs text-muted truncate">{{ auth.email }}</p>
        </div>

        <button
          @click="toggleDarkAction"
          class="w-full text-left px-4 py-2 text-sm text-on-surface hover:bg-surface-container-low dark:hover:bg-slate-700 flex items-center justify-between transition-colors"
        >
          {{ isDark ? "Light Mode" : "Dark Mode" }}
          <svg
            class="w-4 h-4 text-muted"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"
            />
          </svg>
        </button>

        <button
          @click="handleLogout"
          class="w-full text-left px-4 py-2 text-sm text-danger hover:bg-danger-light transition-colors"
        >
          Sign Out
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore();
const { logout } = useAuth();
const { isDark, initTheme, toggle: toggleDark } = useTheme();

const open = ref(false);
const menuRef = ref<HTMLElement | null>(null);

function onOutsideClick(e: Event) {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    open.value = false;
  }
}

const initials = computed(() =>
  (auth.fullName ?? "U")
    .split(" ")
    .slice(0, 2)
    .map((n) => n[0])
    .join("")
    .toUpperCase(),
);

function toggleDarkAction() {
  toggleDark();
  open.value = false;
}

async function handleLogout() {
  open.value = false;
  await logout();
}

// Close on outside click
onMounted(() => {
  document.addEventListener("click", onOutsideClick);
  initTheme();
});

onUnmounted(() => {
  document.removeEventListener("click", onOutsideClick);
});
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.15s,
    transform 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
