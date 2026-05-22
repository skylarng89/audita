import { useAuthStore } from "~/stores/auth";

export default defineNuxtRouteMiddleware(() => {
  const auth = useAuthStore();
  if (auth.role !== "Admin" && auth.role !== "SUPER_ADMIN") {
    return navigateTo("/dashboard");
  }
});
