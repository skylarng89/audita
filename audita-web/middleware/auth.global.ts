import { useAuthStore } from "~/stores/auth";

export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore();

  if (!auth.isAuthenticated && typeof auth.hydrateFromCookie === "function") {
    auth.hydrateFromCookie();
  }

  // Public routes that must remain reachable without a session.
  const publicRoutes = [
    "/auth/sign-in",
    "/auth/forgot-password",
    "/auth/reset-password",
    "/auth/accept-invite",
    "/platform/bootstrap",
    "/setup",
  ];

  if (publicRoutes.some((route) => to.path.startsWith(route))) {
    return;
  }

  if (!auth.isAuthenticated) {
    return navigateTo("/auth/sign-in");
  }
});
