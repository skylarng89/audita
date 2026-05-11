<template>
  <div class="min-h-screen flex flex-col bg-[#f7f9fb] dark:bg-slate-950">
    <!-- Skip to main content — WCAG 2.4.1 -->
    <a href="#main-content" class="skip-link">Skip to main content</a>

    <!-- ── Top Header ─────────────────────────────────────────────────────── -->
    <header
      class="fixed top-0 left-0 right-0 z-30 h-14 border-b border-outline-variant/50 bg-white/95 backdrop-blur-sm dark:bg-slate-900/90 dark:border-border-dark flex items-center px-4 gap-3 shadow-[0_1px_3px_rgba(0,35,111,0.05)]"
    >
      <!-- Mobile: hamburger + logo -->
      <div class="flex items-center gap-2 md:hidden">
        <button
          class="btn-ghost btn-sm w-9 h-9 p-0 rounded-full"
          aria-label="Open navigation menu"
          :aria-expanded="mobileNavOpen"
          aria-controls="mobile-nav"
          @click="mobileNavOpen = true"
        >
          <svg
            class="w-5 h-5"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 6h16M4 12h16M4 18h16"
            />
          </svg>
        </button>
        <div
          class="w-7 h-7 rounded-lg bg-primary flex items-center justify-center text-white font-bold text-xs"
        >
          A
        </div>
        <span class="font-bold text-sm text-primary">Audita</span>
      </div>

      <!-- Desktop: spacer that matches sidebar width -->
      <div class="hidden md:block w-52 shrink-0" aria-hidden="true" />

      <!-- Flexible spacer -->
      <div class="flex-1" />

      <!-- Right controls -->
      <div class="flex items-center gap-1">
        <!-- Dark mode toggle -->
        <ClientOnly>
          <button
            class="btn-ghost btn-sm w-9 h-9 p-0 rounded-full"
            :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
            :aria-pressed="isDark"
            @click="toggleDark"
          >
            <svg
              v-if="isDark"
              class="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"
              />
            </svg>
            <svg
              v-else
              class="w-4 h-4"
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
        </ClientOnly>

        <!-- Notification bell -->
        <SharedAppNotificationBell />

        <!-- Help -->
        <button
          class="btn-ghost btn-sm w-9 h-9 p-0 rounded-full"
          aria-label="Help and documentation"
        >
          <svg
            class="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </button>

        <!-- User menu -->
        <SharedAppUserMenu />
      </div>
    </header>

    <!-- ── Mobile navigation drawer ──────────────────────────────────────── -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="mobileNavOpen"
        id="mobile-nav"
        class="fixed inset-0 z-40 md:hidden"
        role="dialog"
        aria-modal="true"
        aria-label="Navigation menu"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black/40"
          @click="mobileNavOpen = false"
        />

        <!-- Drawer panel -->
        <nav
          class="absolute left-0 top-0 bottom-0 w-72 bg-white dark:bg-slate-950 border-r border-outline-variant/50 dark:border-border-dark flex flex-col overflow-y-auto translate-x-0"
          aria-label="Main navigation"
        >
          <!-- Drawer header -->
          <div
            class="flex items-center justify-between px-4 py-3 border-b border-outline-variant/50 dark:border-border-dark shrink-0"
          >
            <div class="flex items-center gap-2">
              <div
                class="w-7 h-7 rounded-lg bg-primary flex items-center justify-center text-white font-bold text-xs"
              >
                A
              </div>
              <span class="font-bold text-sm text-primary">Audita</span>
            </div>
            <button
              class="btn-ghost btn-sm w-9 h-9 p-0 rounded-full"
              aria-label="Close navigation menu"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
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

          <!-- Nav links -->
          <div class="flex-1 p-3 flex flex-col gap-0.5">
            <NuxtLink
              to="/dashboard"
              class="sidebar-link"
              active-class="sidebar-link-active"
              :aria-current="$route.path === '/dashboard' ? 'page' : undefined"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-4 h-4 shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"
                />
              </svg>
              Dashboard
            </NuxtLink>
            <NuxtLink
              to="/change-requests"
              class="sidebar-link"
              active-class="sidebar-link-active"
              :aria-current="$route.path.startsWith('/change-requests') ? 'page' : undefined"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-4 h-4 shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
              Change Requests
            </NuxtLink>
            <NuxtLink
              v-if="auth.isAdmin"
              to="/users"
              class="sidebar-link"
              active-class="sidebar-link-active"
              :aria-current="$route.path === '/users' ? 'page' : undefined"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-4 h-4 shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                />
              </svg>
              Users
            </NuxtLink>
            <NuxtLink
              to="/groups"
              class="sidebar-link"
              active-class="sidebar-link-active"
              :aria-current="$route.path === '/groups' ? 'page' : undefined"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-4 h-4 shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"
                />
              </svg>
              Groups
            </NuxtLink>
            <NuxtLink
              to="/audit-trail"
              class="sidebar-link"
              active-class="sidebar-link-active"
              :aria-current="$route.path === '/audit-trail' ? 'page' : undefined"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-4 h-4 shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              Audit Trail
            </NuxtLink>
            <NuxtLink
              v-if="auth.isAdmin"
              to="/admin/custom-fields"
              class="sidebar-link"
              active-class="sidebar-link-active"
              :aria-current="$route.path === '/admin/custom-fields' ? 'page' : undefined"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-4 h-4 shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A2 2 0 013 12V7a4 4 0 014-4z"
                />
              </svg>
              Custom Fields
            </NuxtLink>
            <NuxtLink
              v-if="auth.isAdmin"
              to="/admin/settings"
              class="sidebar-link"
              active-class="sidebar-link-active"
              :aria-current="$route.path === '/admin/settings' ? 'page' : undefined"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-4 h-4 shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                />
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                />
              </svg>
              Settings
            </NuxtLink>
          </div>

          <!-- Create button -->
          <div
            class="p-3 border-t border-outline-variant/50 dark:border-border-dark shrink-0"
          >
            <NuxtLink
              v-if="auth.canCreateCR"
              to="/change-requests/new"
              class="btn-primary btn-md w-full shadow-md shadow-primary/20 rounded-xl"
              @click="mobileNavOpen = false"
            >
              <svg
                class="w-4 h-4"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 4v16m8-8H4"
                />
              </svg>
              New Change
            </NuxtLink>
          </div>
        </nav>
      </div>
    </Transition>

    <!-- ── Body: Sidebar + Main ────────────────────────────────────────────── -->
    <div class="flex pt-14 min-h-screen">
      <!-- Sidebar (desktop only) -->
      <SharedAppSidebar />

      <!-- Main content -->
      <main
        id="main-content"
        class="flex-1 ml-0 md:ml-56 p-6 min-w-0"
        tabindex="-1"
      >
        <div class="mx-auto max-w-7xl">
          <slot />
        </div>
      </main>
    </div>

    <!-- Toast container -->
    <SharedAppToastContainer />
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore();
const colorMode = useColorMode();
const route = useRoute();

const isDark = computed(() => colorMode.value === "dark");

function toggleDark() {
  colorMode.preference = isDark.value ? "light" : "dark";
}

const mobileNavOpen = ref(false);

// Close drawer on Escape
onMounted(() => {
  function onEsc(e: KeyboardEvent) {
    if (e.key === "Escape" && mobileNavOpen.value) {
      mobileNavOpen.value = false;
    }
  }
  window.addEventListener("keydown", onEsc);
  onUnmounted(() => window.removeEventListener("keydown", onEsc));
});
</script>
    <header
      class="fixed top-0 left-0 right-0 z-30 h-14 border-b border-outline-variant/50 bg-white/95 backdrop-blur-sm dark:bg-slate-900/90 dark:border-border-dark flex items-center px-4 gap-4 shadow-[0_1px_3px_rgba(0,35,111,0.05)]"
    >
      <!-- Logo (visible on mobile) -->
      <div class="flex items-center gap-2 md:hidden">
        <div
          class="w-7 h-7 rounded-lg bg-primary flex items-center justify-center text-white font-bold text-xs"
        >
          A
        </div>
        <span class="font-bold text-sm text-primary">Audita</span>
      </div>

      <!-- Global Search -->
      <div class="flex-1 max-w-xl mx-auto">
        <div class="relative">
          <span
            class="absolute left-3 top-1/2 -translate-y-1/2 text-muted pointer-events-none"
          >
            <svg
              class="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
          </span>
          <input
            type="search"
            placeholder="Search changes, users, or audits..."
            class="input pl-9 pr-4 py-2 text-sm h-9"
          />
        </div>
      </div>

      <!-- Right controls -->
      <div class="flex items-center gap-1">
        <!-- Notification bell -->
        <SharedAppNotificationBell />

        <!-- Help -->
        <button
          class="btn-ghost btn-sm w-8 h-8 p-0 rounded-full"
          aria-label="Help"
        >
          <svg
            class="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </button>

        <!-- User menu -->
        <SharedAppUserMenu />
      </div>
    </header>

    <!-- ── Body: Sidebar + Main ────────────────────────────────────────────── -->
    <div class="flex pt-14 min-h-screen">
      <!-- Sidebar -->
      <SharedAppSidebar />

      <!-- Main content -->
      <main class="flex-1 ml-0 md:ml-56 p-6 min-w-0">
        <div class="mx-auto max-w-7xl">
          <slot />
        </div>
      </main>
    </div>

    <!-- Toast container -->
    <SharedAppToastContainer />
  </div>
</template>
