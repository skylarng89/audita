<template>
  <nav
    :class="[
      'fixed left-0 top-14 bottom-0 border-r border-outline-variant/50 bg-white dark:border-border-dark dark:bg-slate-950 flex flex-col z-20 hidden md:flex transition-[width] duration-200 overflow-hidden',
      sidebarCollapsed ? 'w-14' : 'w-56',
    ]"
    aria-label="Main navigation"
  >
    <!-- Nav links -->
    <div
      :class="[
        'flex-1 flex flex-col gap-0.5 overflow-y-auto',
        sidebarCollapsed ? 'p-2' : 'p-3',
      ]"
    >
      <NuxtLink
        to="/dashboard"
        :class="sidebarCollapsed ? 'sidebar-link-rail' : 'sidebar-link'"
        :active-class="
          sidebarCollapsed ? 'sidebar-link-rail-active' : 'sidebar-link-active'
        "
        title="Dashboard"
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
        <span v-if="!sidebarCollapsed">Dashboard</span>
      </NuxtLink>

      <NuxtLink
        to="/change-requests"
        :class="sidebarCollapsed ? 'sidebar-link-rail' : 'sidebar-link'"
        :active-class="
          sidebarCollapsed ? 'sidebar-link-rail-active' : 'sidebar-link-active'
        "
        title="Change Requests"
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
        <span v-if="!sidebarCollapsed">Change Requests</span>
      </NuxtLink>

      <NuxtLink
        v-if="auth.isAdmin"
        to="/users"
        :class="sidebarCollapsed ? 'sidebar-link-rail' : 'sidebar-link'"
        :active-class="
          sidebarCollapsed ? 'sidebar-link-rail-active' : 'sidebar-link-active'
        "
        title="Users"
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
        <span v-if="!sidebarCollapsed">Users</span>
      </NuxtLink>

      <NuxtLink
        to="/groups"
        :class="sidebarCollapsed ? 'sidebar-link-rail' : 'sidebar-link'"
        :active-class="
          sidebarCollapsed ? 'sidebar-link-rail-active' : 'sidebar-link-active'
        "
        title="Groups"
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
        <span v-if="!sidebarCollapsed">Groups</span>
      </NuxtLink>

      <NuxtLink
        v-if="auth.isAdmin || auth.isAuditor"
        to="/audit-trail"
        :class="sidebarCollapsed ? 'sidebar-link-rail' : 'sidebar-link'"
        :active-class="
          sidebarCollapsed ? 'sidebar-link-rail-active' : 'sidebar-link-active'
        "
        title="Audit Trail"
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
        <span v-if="!sidebarCollapsed">Audit Trail</span>
      </NuxtLink>

      <NuxtLink
        v-if="auth.isAdmin"
        to="/admin/custom-fields"
        :class="sidebarCollapsed ? 'sidebar-link-rail' : 'sidebar-link'"
        :active-class="
          sidebarCollapsed ? 'sidebar-link-rail-active' : 'sidebar-link-active'
        "
        title="Custom Fields"
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
        <span v-if="!sidebarCollapsed">Custom Fields</span>
      </NuxtLink>

      <NuxtLink
        v-if="auth.isAdmin"
        to="/admin/settings"
        :class="sidebarCollapsed ? 'sidebar-link-rail' : 'sidebar-link'"
        :active-class="
          sidebarCollapsed ? 'sidebar-link-rail-active' : 'sidebar-link-active'
        "
        title="Settings"
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
        <span v-if="!sidebarCollapsed">Settings</span>
      </NuxtLink>
    </div>

    <!-- Footer: Create CTA + collapse toggle -->
    <div
      :class="[
        'border-t border-outline-variant/50 dark:border-border-dark shrink-0',
        sidebarCollapsed ? 'p-2' : 'p-3',
      ]"
    >
      <NuxtLink
        v-if="auth.canCreateCR && !sidebarCollapsed"
        to="/change-requests/new"
        class="btn-primary btn-md w-full shadow-md shadow-primary/20 rounded-xl mb-3"
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
      <NuxtLink
        v-else-if="auth.canCreateCR"
        to="/change-requests/new"
        class="flex items-center justify-center w-10 h-10 mx-auto mb-2 rounded-xl bg-primary text-white hover:bg-primary-dark shadow-sm transition-colors"
        title="New Change Request"
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
      </NuxtLink>

      <template v-if="!sidebarCollapsed">
        <div class="mt-1 space-y-1 px-1">
          <p class="text-[11px] text-muted">Documentation</p>
          <p class="text-[11px] text-muted">Support</p>
        </div>
      </template>

      <button
        class="mt-2 w-full flex items-center justify-center gap-2 rounded-lg px-2 py-1.5 text-xs text-muted hover:bg-surface-container hover:text-on-surface transition-colors dark:hover:bg-slate-800"
        :aria-label="sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'"
        @click="toggle"
      >
        <svg
          class="w-4 h-4 shrink-0 transition-transform duration-200"
          :class="sidebarCollapsed ? 'rotate-180' : ''"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M11 19l-7-7 7-7m8 14l-7-7 7-7"
          />
        </svg>
        <span v-if="!sidebarCollapsed" class="text-xs">Collapse</span>
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
const auth = useAuthStore();

const sidebarCollapsed = useState("sidebarCollapsed", () => false);

function applyDataAttr(collapsed: boolean) {
  if (collapsed) {
    document.documentElement.dataset.sidebarCollapsed = "";
  } else {
    delete document.documentElement.dataset.sidebarCollapsed;
  }
}

function toggle() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  localStorage.setItem("sidebarCollapsed", String(sidebarCollapsed.value));
  applyDataAttr(sidebarCollapsed.value);
}

onMounted(() => {
  const stored = localStorage.getItem("sidebarCollapsed");
  if (stored !== null) {
    sidebarCollapsed.value = stored === "true";
  }
  applyDataAttr(sidebarCollapsed.value);
});
</script>
