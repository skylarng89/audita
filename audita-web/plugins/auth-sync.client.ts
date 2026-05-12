import {
  AUTH_SESSION_SYNC_CHANNEL,
  type AuthSessionSyncEvent,
} from "~/composables/authSessionSync";
import { restoreSessionFromCookie } from "~/composables/sessionRestore";
import { useAuthStore } from "~/stores/auth";

export default defineNuxtPlugin(() => {
  const auth = useAuthStore();
  const config = useRuntimeConfig();
  const tabId = useState<string>("auth-sync-tab-id", () => crypto.randomUUID());

  async function handleEvent(event: AuthSessionSyncEvent) {
    if (event.sourceTabId === tabId.value) {
      return;
    }

    if (event.type === "session-logged-out") {
      auth.clearAuth();
      await navigateTo("/auth/sign-in");
      return;
    }

    try {
      await restoreSessionFromCookie(
        auth,
        config.public.apiContractVersion,
        false,
      );
    } catch {
      auth.clearAuth();
    }
  }

  if (typeof BroadcastChannel !== "undefined") {
    const channel = new BroadcastChannel(AUTH_SESSION_SYNC_CHANNEL);
    channel.onmessage = (message: MessageEvent<AuthSessionSyncEvent>) => {
      void handleEvent(message.data);
    };
    return;
  }

  window.addEventListener("storage", (event) => {
    if (event.key !== AUTH_SESSION_SYNC_CHANNEL || !event.newValue) {
      return;
    }

    const payload = JSON.parse(event.newValue) as AuthSessionSyncEvent;
    void handleEvent(payload);
  });
});
