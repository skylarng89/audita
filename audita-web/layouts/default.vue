<template>
  <div class="min-h-screen flex flex-col dark:bg-slate-950">

    <!-- ── Top Header ─────────────────────────────────────────────────────── -->
    <header class="fixed top-0 left-0 right-0 z-30 h-14 bg-white dark:bg-surface-dark border-b border-border dark:border-border-dark flex items-center px-4 gap-4">

      <!-- Logo (visible on mobile) -->
      <div class="flex items-center gap-2 md:hidden">
        <div class="w-7 h-7 rounded-full bg-primary flex items-center justify-center text-white font-bold text-xs">A</div>
        <span class="font-bold text-sm text-primary">Audita</span>
      </div>

      <!-- Global Search -->
      <div class="flex-1 max-w-xl mx-auto">
        <div class="relative">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 text-muted">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </span>
          <input
            type="search"
            placeholder="Search changes, users, or audits..."
            class="input pl-9 pr-4 py-1.5 text-sm"
          />
        </div>
      </div>

      <!-- Right controls -->
      <div class="flex items-center gap-2">
        <!-- Notification bell -->
        <AppNotificationBell />

        <!-- Help -->
        <button class="btn-ghost btn-sm w-8 h-8 p-0 rounded-full" aria-label="Help">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3
                     0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093
                     m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </button>

        <!-- User menu -->
        <AppUserMenu />
      </div>
    </header>

    <!-- ── Body: Sidebar + Main ────────────────────────────────────────────── -->
    <div class="flex pt-14 min-h-screen">

      <!-- Sidebar -->
      <AppSidebar />

      <!-- Main content -->
      <main class="flex-1 ml-0 md:ml-56 p-6 min-w-0">
        <slot />
      </main>
    </div>

    <!-- Toast container -->
    <AppToastContainer />
  </div>
</template>
