import { useAuthStore } from "~/stores/auth";

export default defineNuxtRouteMiddleware(() => {
  const auth = useAuthStore();
  if (!auth.hasPermission("cr.create")) {
    return navigateTo("/change-requests");
  }
});
