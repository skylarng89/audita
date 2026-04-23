import { defineStore } from 'pinia'
import type { AuthResponse, UserRole } from '~/types'

interface AuthState {
  accessToken: string | null
  userId: string | null
  email: string | null
  fullName: string | null
  role: UserRole | null
  tenantSlug: string | null
}

export const useAuthStore = defineStore('auth', {
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
    isSuperAdmin: (s) => s.role === 'SUPER_ADMIN',
    isAdmin: (s) => s.role === 'Admin',
    isAuditor: (s) => s.role === 'Auditor',
    canApprove: (s) => s.role === 'Admin' || s.role === 'Approver',
    canCreateCR: (s) => s.role === 'Admin' || s.role === 'Requester',
  },

  actions: {
    setAuth(response: AuthResponse) {
      this.accessToken = response.accessToken
      this.userId = response.userId
      this.email = response.email
      this.fullName = response.fullName
      this.role = response.role
      this.tenantSlug = response.tenantSlug
    },

    clearAuth() {
      this.accessToken = null
      this.userId = null
      this.email = null
      this.fullName = null
      this.role = null
      this.tenantSlug = null
    },

    // Called by the API plugin on 401 responses
    async logout() {
      try {
        await $fetch('/api/v1/auth/logout', { method: 'POST' })
      } catch {}
      this.clearAuth()
      await navigateTo('/auth/sign-in')
    },
  },
})
