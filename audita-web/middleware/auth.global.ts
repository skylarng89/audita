import { useAuthStore } from "~/stores/auth";

export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore();

  // Public routes that must remain reachable without a session.
  const publicRoutes = [
    "/auth/sign-in",
    "/auth/forgot-password",
    "/auth/reset-password",
    "/auth/accept-invite",
    "/platform/bootstrap",
  ];

  if (publicRoutes.some((route) => to.path.startsWith(route))) {
    return;
  }

  if (!auth.isAuthenticated) {
    return navigateTo("/auth/sign-in");
  }
});
