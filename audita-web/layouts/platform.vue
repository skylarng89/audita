<template>
  <div class="min-h-screen flex flex-col bg-[#f7f9fb] dark:bg-slate-950">
    <!-- Header -->
    <header
      class="fixed top-0 left-0 right-0 z-30 h-14 border-b border-outline-variant/50 bg-white/95 backdrop-blur-sm dark:bg-slate-900/90 dark:border-border-dark flex items-center px-6 gap-4 shadow-[0_1px_3px_rgba(0,35,111,0.05)]"
    >
      <span class="font-bold text-primary text-sm tracking-tight">Audita ITSM</span>

      <div class="flex-1 max-w-xl">
        <div class="relative">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 text-muted pointer-events-none">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </span>
          <input type="search" placeholder="Search across tenants..." class="input pl-9 py-2 text-sm h-9" />
        </div>
      </div>

      <div class="flex items-center gap-2 ml-auto">
        <button class="btn-ghost btn-sm w-8 h-8 p-0 rounded-full">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
        </button>
        <button class="btn-ghost btn-sm w-8 h-8 p-0 rounded-full">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </button>
        <div class="flex items-center gap-2 pl-1">
          <div class="text-right hidden sm:block">
            <p class="text-xs font-semibold text-on-surface leading-none">Super Admin</p>
            <p class="text-[10px] text-muted mt-0.5">Platform Controller</p>
          </div>
          <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white text-xs font-bold shadow-sm">SA</div>
        </div>
      </div>
    </header>

    <div class="flex pt-14 min-h-screen">
      <nav
        class="fixed left-0 top-14 bottom-0 w-56 bg-white dark:bg-slate-950 border-r border-outline-variant/50 dark:border-border-dark flex flex-col p-4 gap-1 overflow-y-auto"
      >
        <div class="flex items-center gap-2 px-3 py-2 mb-4">
          <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center shadow-sm">
            <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <div>
            <p class="text-xs font-bold text-primary uppercase tracking-wider">Control Center</p>
            <p class="text-[10px] text-muted">ITIL V4 Compliant</p>
          </div>
        </div>

        <NuxtLink to="/platform" class="sidebar-link" active-class="sidebar-link-active">Command Center</NuxtLink>
        <NuxtLink to="/platform/tenants" class="sidebar-link" active-class="sidebar-link-active">Tenant Management</NuxtLink>
        <NuxtLink to="/platform/audit" class="sidebar-link" active-class="sidebar-link-active">Audit Logs</NuxtLink>

        <div class="mt-auto pt-4 border-t border-outline-variant/50 dark:border-border-dark space-y-1">
          <NuxtLink to="/platform/tenants/new" class="btn-primary btn-md w-full rounded-xl shadow-md shadow-primary/20 text-sm">
            + Provision New Org
          </NuxtLink>
          <button @click="logout" class="sidebar-link w-full text-left text-danger hover:bg-danger-light hover:text-danger mt-2">
            <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
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
