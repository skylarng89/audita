import { useAuthStore } from "~/stores/auth";

const PUBLIC_ROUTES = [
  "/auth/forgot-password",
  "/auth/reset-password",
  "/auth/accept-invite",
  "/platform/bootstrap",
];

async function resolveRootRoute(auth: ReturnType<typeof useAuthStore>) {
  if (auth.isAuthenticated) {
    return auth.isSuperAdmin ? "/platform" : "/dashboard";
  }
  const { fetchStatus } = useOnboarding();
  const status = await fetchStatus();
  return status !== null && !status.onboardingCompleted
    ? "/setup"
    : "/auth/sign-in";
}

async function resolveSignInRoute(auth: ReturnType<typeof useAuthStore>) {
  const { fetchStatus } = useOnboarding();
  const status = await fetchStatus();
  if (status !== null && !status.onboardingCompleted) {
    return "/setup";
  }
  if (status?.tenantSlug && !auth.tenantSlug) {
    auth.tenantSlug = status.tenantSlug;
  }
  return null;
}

async function resolveSetupRoute() {
  const { fetchStatus } = useOnboarding();
  const status = await fetchStatus();
  return status?.onboardingCompleted ? "/auth/sign-in" : null;
}

export default defineNuxtRouteMiddleware(async (to) => {
  const auth = useAuthStore();

  if (to.path === "/") {
    return navigateTo(await resolveRootRoute(auth));
  }

  if (to.path === "/auth/sign-in") {
    const redirect = await resolveSignInRoute(auth);
    if (redirect) return navigateTo(redirect);
    return;
  }

  if (to.path === "/setup") {
    const redirect = await resolveSetupRoute();
    if (redirect) return navigateTo(redirect);
    return;
  }

  if (PUBLIC_ROUTES.some((route) => to.path.startsWith(route))) {
    return;
  }

  if (!auth.isAuthenticated) {
    return navigateTo("/auth/sign-in");
  }
});
