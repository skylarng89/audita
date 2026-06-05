import type { UserRole } from "~/types"

export default defineNuxtRouteMiddleware(() => {
  const { isAllowed, redirect } = useRoleGuard(["Admin", "SUPER_ADMIN"] as UserRole[])
  if (!isAllowed.value) {
    return redirect()
  }
})
