import { defineStore } from "pinia";
import type { AuthResponse, UserRole } from "~/types";

const AUTH_COOKIE_KEY = "audita_auth";

interface PersistedAuth {
  accessToken: string | null;
  userId: string | null;
  email: string | null;
  fullName: string | null;
  role: UserRole | null;
  tenantSlug: string | null;
}

function useAuthCookie() {
  return useCookie<PersistedAuth | null>(AUTH_COOKIE_KEY, {
    path: "/",
    sameSite: "lax",
  });
}

interface AuthState {
  accessToken: string | null;
  userId: string | null;
  email: string | null;
  fullName: string | null;
  role: UserRole | null;
  tenantSlug: string | null;
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    accessToken: null,
    userId: null,
    email: null,
    fullName: null,
    role: null,
    tenantSlug: null,
  }),

  getters: {
    isAuthenticated: (s) => !!s.accessToken,
    isSuperAdmin: (s) => s.role === "SUPER_ADMIN",
    isAdmin: (s) => s.role === "Admin",
    isAuditor: (s) => s.role === "Auditor",
    canApprove: (s) => s.role === "Admin" || s.role === "Approver",
    canCreateCR: (s) => s.role === "Admin" || s.role === "Requester",
  },

  actions: {
    hydrateFromCookie() {
      const persisted = useAuthCookie();
      const value = persisted.value;

      if (!value || !value.accessToken) return;

      this.accessToken = value.accessToken;
      this.userId = value.userId;
      this.email = value.email;
      this.fullName = value.fullName;
      this.role = value.role;
      this.tenantSlug = value.tenantSlug;
    },

    persistToCookie() {
      const persisted = useAuthCookie();
      persisted.value = {
        accessToken: this.accessToken,
        userId: this.userId,
        email: this.email,
        fullName: this.fullName,
        role: this.role,
        tenantSlug: this.tenantSlug,
      };
    },

    setAuth(response: AuthResponse) {
      this.accessToken = response.accessToken;
      this.userId = response.userId;
      this.email = response.email;
      this.fullName = response.fullName;
      this.role = response.role;
      this.tenantSlug = response.tenantSlug;
      this.persistToCookie();
    },

    setAccessToken(accessToken: string) {
      this.accessToken = accessToken;
      this.persistToCookie();
    },

    clearAuth() {
      this.accessToken = null;
      this.userId = null;
      this.email = null;
      this.fullName = null;
      this.role = null;
      this.tenantSlug = null;

      const persisted = useAuthCookie();
      persisted.value = null;
    },

    // Called by the API plugin on 401 responses
    async logout() {
      try {
        await $fetch("/api/v1/auth/logout", { method: "POST" });
      } catch {}
      this.clearAuth();
      await navigateTo("/auth/sign-in");
    },
  },
});
