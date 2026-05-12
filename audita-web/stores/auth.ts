import { defineStore } from "pinia";
import { createAuthSessionSyncEvent } from "~/composables/authSessionSync";
import {
  computeTokenExpiresAt,
  hasActiveAccessToken,
} from "~/composables/authSession";
import type { AuthResponse, UserRole } from "~/types";

interface AuthState {
  accessToken: string | null;
  tokenExpiresAt: number | null;
  userId: string | null;
  email: string | null;
  fullName: string | null;
  role: UserRole | null;
  tenantSlug: string | null;
  sessionInitialized: boolean;
}

interface AuthMutationOptions {
  broadcast?: boolean;
}

function broadcastAuthEvent(type: "session-restored" | "session-logged-out") {
  if (!import.meta.client) {
    return;
  }

  const tabId = useState<string>("auth-sync-tab-id", () => crypto.randomUUID());
  const event = createAuthSessionSyncEvent(type, tabId.value);

  if (typeof BroadcastChannel !== "undefined") {
    const channel = new BroadcastChannel("audita-auth-session");
    channel.postMessage(event);
    channel.close();
    return;
  }

  localStorage.setItem("audita-auth-session", JSON.stringify(event));
  localStorage.removeItem("audita-auth-session");
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    accessToken: null,
    tokenExpiresAt: null,
    userId: null,
    email: null,
    fullName: null,
    role: null,
    tenantSlug: null,
    sessionInitialized: false,
  }),

  getters: {
    isAuthenticated: (s) =>
      hasActiveAccessToken(s.accessToken, s.tokenExpiresAt),
    isSuperAdmin: (s) => s.role === "SUPER_ADMIN",
    isAdmin: (s) => s.role === "Admin",
    isAuditor: (s) => s.role === "Auditor",
    canApprove: (s) => s.role === "Admin" || s.role === "Approver",
    canCreateCR: (s) => s.role === "Admin" || s.role === "Requester",
  },

  actions: {
    invalidateCachedSessionState() {
      const { invalidateStatus } = useOnboarding();
      invalidateStatus();
    },

    markSessionInitialized() {
      this.sessionInitialized = true;
    },

    setTenantSlug(tenantSlug: string | null) {
      this.tenantSlug = tenantSlug;
    },

    setAuth(response: AuthResponse, options: AuthMutationOptions = {}) {
      this.accessToken = response.accessToken;
      this.tokenExpiresAt = computeTokenExpiresAt(response.expiresIn);
      this.userId = response.userId;
      this.email = response.email;
      this.fullName = response.fullName;
      this.role = response.role;
      this.tenantSlug = response.tenantSlug;
      this.sessionInitialized = true;
      this.invalidateCachedSessionState();

      if (options.broadcast) {
        broadcastAuthEvent("session-restored");
      }
    },

    clearAuth(options: AuthMutationOptions = {}) {
      this.accessToken = null;
      this.tokenExpiresAt = null;
      this.userId = null;
      this.email = null;
      this.fullName = null;
      this.role = null;
      this.tenantSlug = null;
      this.sessionInitialized = true;
      this.invalidateCachedSessionState();

      if (options.broadcast) {
        broadcastAuthEvent("session-logged-out");
      }
    },

    // Called by the API plugin on 401 responses
    async logout(options: AuthMutationOptions = {}) {
      try {
        await $fetch("/api/v1/auth/logout", { method: "POST" });
      } catch (error) {
        console.error("Logout API failed", error);
      }
      this.clearAuth({ broadcast: options.broadcast ?? true });
      await navigateTo("/auth/sign-in");
    },
  },
});
