<template>
  <div class="min-h-screen flex flex-col bg-slate-100 dark:bg-slate-950">
    <!-- Header -->
    <header
      class="fixed top-0 left-0 right-0 z-30 h-14 border-b border-border bg-white/95 backdrop-blur dark:bg-slate-900/90 dark:border-border-dark flex items-center px-6 gap-4"
    >
      <span class="font-bold text-primary text-sm tracking-tight"
        >Audita ITSM</span
      >

      <div class="flex-1 max-w-xl">
        <input
          type="search"
          placeholder="Search across tenants..."
          class="input py-1.5 text-sm"
        />
      </div>

      <div class="flex items-center gap-3 ml-auto">
        <button class="btn-ghost btn-sm">
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
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
            />
          </svg>
        </button>

        <span class="text-sm font-semibold">Super Admin</span>
        <div
          class="w-8 h-8 rounded-full bg-primary flex items-center justify-center text-white text-xs font-bold"
        >
          SA
        </div>
      </div>
    </header>

    <div class="flex pt-14 min-h-screen">
      <nav
        class="fixed left-0 top-14 bottom-0 w-56 bg-slate-50/95 dark:bg-slate-950/95 border-r border-border dark:border-border-dark flex flex-col p-4 gap-1 overflow-y-auto backdrop-blur"
      >
        <div class="flex items-center gap-2 px-3 py-2 mb-4">
          <div
            class="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center"
          >
            <svg
              class="w-4 h-4 text-primary"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
              />
            </svg>
          </div>
          <div>
            <p class="text-xs font-bold text-primary uppercase tracking-wider">
              The Sovereign
            </p>
            <p class="text-xs text-muted">Super Admin Access</p>
          </div>
        </div>

        <NuxtLink
          to="/platform"
          class="sidebar-link"
          active-class="sidebar-link-active"
          >Command Center</NuxtLink
        >
        <NuxtLink
          to="/platform/tenants"
          class="sidebar-link"
          active-class="sidebar-link-active"
          >Tenant Management</NuxtLink
        >
        <NuxtLink
          to="/platform/audit"
          class="sidebar-link"
          active-class="sidebar-link-active"
          >Audit Logs</NuxtLink
        >

        <div
          class="mt-auto pt-4 border-t border-border dark:border-border-dark"
        >
          <button @click="logout" class="sidebar-link w-full text-left">
            Log Out
          </button>
        </div>
      </nav>

      <main class="flex-1 ml-56 p-6 min-w-0">
        <div class="mx-auto max-w-7xl">
          <slot />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
const { logout } = useAuth();
</script>
