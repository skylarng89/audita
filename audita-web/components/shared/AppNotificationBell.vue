<template>
  <div class="relative" ref="bellRef">
    <button
      @click="open = !open"
      class="relative btn-ghost btn-sm w-8 h-8 p-0 rounded-full"
      aria-label="Notifications"
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
          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
        />
      </svg>
      <span
        v-if="notifStore.unreadCount > 0"
        class="absolute -top-0.5 -right-0.5 w-4 h-4 bg-danger rounded-full text-white text-[10px] font-bold flex items-center justify-center"
      >
        {{ notifStore.unreadCount > 9 ? "9+" : notifStore.unreadCount }}
      </span>
    </button>

    <Transition name="fade">
      <div
        v-if="open"
        class="absolute right-0 mt-2 w-80 bg-white dark:bg-slate-800 shadow-card-md rounded-xl overflow-hidden z-50 border border-outline-variant/50 dark:border-border-dark"
      >
        <div
          class="flex items-center justify-between px-4 py-3 border-b border-outline-variant/40 dark:border-border-dark"
        >
          <h3 class="font-semibold text-sm text-on-surface">Notifications</h3>
          <button
            v-if="notifStore.unreadCount > 0"
            @click="markAllRead"
            class="text-xs text-primary hover:underline"
          >
            Mark all read
          </button>
        </div>

        <div
          class="max-h-80 overflow-y-auto divide-y divide-outline-variant/30 dark:divide-border-dark"
        >
          <div
            v-for="n in notifStore.items.slice(0, 10)"
            :key="n.id"
            @click="handleClick(n)"
            class="flex gap-3 px-4 py-3 hover:bg-surface-container-low dark:hover:bg-slate-800 cursor-pointer transition-colors"
            :class="{ 'bg-primary/5 dark:bg-slate-800/60': !n.isRead }"
          >
            <div
              class="w-2 h-2 rounded-full mt-1.5 shrink-0"
              :class="n.isRead ? 'bg-transparent' : 'bg-primary'"
            />
            <div class="min-w-0">
              <p class="text-sm font-medium truncate">{{ n.title }}</p>
              <p class="text-xs text-muted mt-0.5 line-clamp-2">{{ n.body }}</p>
            </div>
          </div>

          <div
            v-if="notifStore.items.length === 0"
            class="px-4 py-8 text-center text-sm text-muted"
          >
            No notifications yet.
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import type { Notification } from "~/types";

const notifStore = useNotificationStore();
const api = useApi();
const open = ref(false);
const bellRef = ref<HTMLElement | null>(null);

function onOutsideClick(e: Event) {
  if (bellRef.value && !bellRef.value.contains(e.target as Node)) {
    open.value = false;
  }
}

async function handleClick(n: Notification) {
  if (!n.isRead) {
    await api(`/api/v1/notifications/${n.id}/read`, { method: "PATCH" });
    notifStore.markRead(n.id);
  }
  open.value = false;
  if (n.link) await navigateTo(n.link);
}

async function markAllRead() {
  await api("/api/v1/notifications/read-all", { method: "POST" });
  notifStore.markAllRead();
}

onMounted(() => {
  api<Notification[]>("/api/v1/notifications")
    .then((items) => {
      notifStore.setItems(items);
      notifStore.setUnreadCount(items.filter((item) => !item.isRead).length);
    })
    .catch(() => {
      // Notification hydration is best-effort.
    });

  document.addEventListener("click", onOutsideClick);
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
