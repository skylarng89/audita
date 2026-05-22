import { useAuthStore } from "~/stores/auth";

export default defineNuxtRouteMiddleware(() => {
  const auth = useAuthStore();
  if (auth.role === "Auditor") {
    return navigateTo("/change-requests");
  }
});
