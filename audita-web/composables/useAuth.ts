import { useAuthStore } from '~/stores/auth'
import type { AuthResponse } from '~/types'

export function useAuth() {
  const auth = useAuthStore()
  const api = useApi()

  async function login(email: string, password: string, tenantSlug?: string) {
    const headers: Record<string, string> = {}
    if (tenantSlug) headers['X-Tenant-Slug'] = tenantSlug

    const response = await api<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: { email, password },
      headers,
    })

    auth.setAuth(response)

    // Role-based redirect
    if (response.role === 'SUPER_ADMIN') {
      await navigateTo('/platform')
    } else {
      await navigateTo('/dashboard')
    }
  }

  async function logout() {
    await auth.logout()
  }

  async function forgotPassword(email: string) {
    return api('/api/v1/auth/forgot-password', {
      method: 'POST',
      body: { email },
    })
  }

  async function resetPassword(token: string, newPassword: string) {
    return api('/api/v1/auth/reset-password', {
      method: 'POST',
      body: { token, newPassword },
    })
  }

  async function acceptInvite(token: string, fullName: string, password: string) {
    return api('/api/v1/auth/accept-invite', {
      method: 'POST',
      body: { token, fullName, password },
    })
  }

  return { login, logout, forgotPassword, resetPassword, acceptInvite }
}
