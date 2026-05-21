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
  const api = useApi();

  let eventSource: EventSource | null = null;
  let intentionalDisconnect = false;

  function buildStreamUrl(streamToken: string) {
    // EventSource is a native browser API — it cannot go through the Nuxt $fetch
    // proxy, so we use a root-relative path (/api/...) which the browser sends
    // to the same origin (the Nuxt server at :7100), and the server-side proxy
    // forwards it to the API container. An absolute http://api-host URL would
    // be CORS-blocked because the browser makes the request directly.
    const token = encodeURIComponent(streamToken);
    return `/api/v1/notifications/stream?streamToken=${token}`;
  }

  function connect() {
    if (!auth.isAuthenticated || eventSource?.readyState === EventSource.OPEN)
      return;

    intentionalDisconnect = false;

    api<{ streamToken: string }>("/api/v1/notifications/stream-token", {
      method: "POST",
    })
      .then(({ streamToken }) => {
        const url = buildStreamUrl(streamToken);
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
          if (intentionalDisconnect) {
            return;
          }
          // Reconnect after 5 seconds if still authenticated
          setTimeout(() => {
            if (auth.isAuthenticated) connect();
          }, 5000);
        };
      })
      .catch(() => {
        notifStore.setConnected(false);
      });
  }

  function disconnect() {
    intentionalDisconnect = true;
    eventSource?.close();
    eventSource = null;
    notifStore.setConnected(false);
  }

  if (import.meta.client) {
    globalThis.addEventListener("beforeunload", disconnect);
    globalThis.addEventListener("pagehide", disconnect);
    globalThis.addEventListener("visibilitychange", () => {
      if (document.visibilityState === "hidden") {
        disconnect();
      } else if (auth.isAuthenticated) {
        connect();
      }
    });
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
