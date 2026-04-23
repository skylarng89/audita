<template>
  <div class="relative" ref="menuRef">
    <button
      @click="open = !open"
      class="flex items-center gap-2 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 p-1.5 transition-colors"
    >
      <div class="w-7 h-7 rounded-full bg-primary flex items-center justify-center text-white text-xs font-bold uppercase">
        {{ initials }}
      </div>
      <div class="hidden sm:block text-left">
        <p class="text-sm font-semibold leading-none text-gray-900 dark:text-gray-100">{{ auth.fullName }}</p>
        <p class="text-xs text-muted mt-0.5">{{ auth.role }}</p>
      </div>
    </button>

    <Transition name="fade">
      <div
        v-if="open"
        class="absolute right-0 mt-2 w-52 card shadow-xl rounded-lg overflow-hidden z-50 py-1"
      >
        <div class="px-4 py-2 border-b border-border dark:border-border-dark">
          <p class="text-sm font-semibold">{{ auth.fullName }}</p>
          <p class="text-xs text-muted truncate">{{ auth.email }}</p>
        </div>

        <button
          @click="toggleDark"
          class="w-full text-left px-4 py-2 text-sm hover:bg-gray-50 dark:hover:bg-slate-700 flex items-center justify-between"
        >
          {{ isDark ? 'Light Mode' : 'Dark Mode' }}
          <svg class="w-4 h-4 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
          </svg>
        </button>

        <button
          @click="handleLogout"
          class="w-full text-left px-4 py-2 text-sm text-danger hover:bg-danger-light"
        >
          Sign Out
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const { logout } = useAuth()

const open = ref(false)
const isDark = ref(false)
const menuRef = ref<HTMLElement | null>(null)

const initials = computed(() =>
  (auth.fullName ?? 'U')
    .split(' ')
    .slice(0, 2)
    .map(n => n[0])
    .join('')
    .toUpperCase()
)

function toggleDark() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('audita-theme', isDark.value ? 'dark' : 'light')
  open.value = false
}

async function handleLogout() {
  open.value = false
  await logout()
}

// Close on outside click
onMounted(() => {
  document.addEventListener('click', (e) => {
    if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
      open.value = false
    }
  })

  // Restore persisted theme
  const saved = localStorage.getItem('audita-theme')
  isDark.value = saved === 'dark'
  document.documentElement.classList.toggle('dark', isDark.value)
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s, transform 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
