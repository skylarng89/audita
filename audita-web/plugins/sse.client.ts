/**
 * SSE client plugin — runs on the client only (.client.ts).
 * Connects to /api/v1/notifications/stream after login.
 * Pushes incoming events to the notification store.
 */
import { useAuthStore } from "~/stores/auth";
import { useNotificationStore } from "~/stores/notifications";
import type { Notification } from "~/types";

export default defineNuxtPlugin(() => {
  const auth = useAuthStore();
  const notifStore = useNotificationStore();
  const config = useRuntimeConfig();

  let eventSource: EventSource | null = null;

  function connect() {
    if (!auth.isAuthenticated || eventSource?.readyState === EventSource.OPEN)
      return;

    if (!auth.accessToken) {
      return;
    }

    const token = encodeURIComponent(auth.accessToken);
    const url = `${config.public.apiBase}/api/v1/notifications/stream?accessToken=${token}`;
    eventSource = new EventSource(url, { withCredentials: true });

    eventSource.onopen = () => {
      notifStore.setConnected(true);
    };

    eventSource.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        const notification: Notification = {
          id: payload.id,
          type: payload.type,
          title: payload.title ?? null,
          body: payload.body ?? null,
          link: payload.link ?? null,
          isRead: payload.isRead ?? payload.read ?? false,
          createdAt: payload.createdAt,
        };
        notifStore.prependNotification(notification);
      } catch {
        // Non-JSON heartbeat or unknown event — ignore
      }
    };

    eventSource.onerror = () => {
      notifStore.setConnected(false);
      eventSource?.close();
      eventSource = null;
      // Reconnect after 5 seconds if still authenticated
      setTimeout(() => {
        if (auth.isAuthenticated) connect();
      }, 5000);
    };
  }

  function disconnect() {
    eventSource?.close();
    eventSource = null;
    notifStore.setConnected(false);
  }

  // Connect when auth state changes to authenticated
  watch(
    () => auth.isAuthenticated,
    (authenticated) => {
      if (authenticated) {
        connect();
      } else {
        disconnect();
      }
    },
    { immediate: true },
  );

  return {
    provide: { sseConnect: connect, sseDisconnect: disconnect },
  };
});
