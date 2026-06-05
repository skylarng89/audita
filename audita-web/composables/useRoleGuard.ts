import { useAuthStore } from "~/stores/auth"
import type { UserRole } from "~/types"

export function useRoleGuard(requiredRoles: UserRole[]) {
  const auth = useAuthStore()

  const isAllowed = computed(() => {
    if (!requiredRoles.length) return true
    return requiredRoles.some(
      (role) => auth.role?.toUpperCase() === role.toUpperCase(),
    )
  })

  const redirect = () => navigateTo("/dashboard")

  return { isAllowed, redirect }
}
