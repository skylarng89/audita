import { useAuthStore } from "~/stores/auth";

export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore();

  if (!auth.isAuthenticated && typeof auth.hydrateFromCookie === "function") {
    auth.hydrateFromCookie();
  }

  // Allow public routes without a session
  const publicRoutes = [
    "/auth/sign-in",
    "/auth/forgot-password",
    "/auth/reset-password",
    "/auth/accept-invite",
  ];

  if (publicRoutes.some((r) => to.path.startsWith(r))) {
    return;
  }

  if (!auth.isAuthenticated) {
    return navigateTo("/auth/sign-in");
  }
});
