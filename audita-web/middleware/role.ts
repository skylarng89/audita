import { useAuthStore } from '~/stores/auth'
import type { UserRole } from '~/types'

export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore()
  const requiredRole = to.meta.requiredRole as UserRole | undefined

  if (requiredRole && auth.role !== requiredRole) {
    return navigateTo('/dashboard')
  }
})
