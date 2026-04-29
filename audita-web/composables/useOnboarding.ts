export interface OnboardingStatusResponse {
  onboardingCompleted: boolean;
  tenantSlug?: string | null;
}

export function useOnboarding() {
  const api = useApi();

  async function fetchStatus() {
    try {
      return await api<OnboardingStatusResponse>(
        "/api/platform/v1/bootstrap/status",
        {
          method: "GET",
          credentials: "omit",
        },
      );
    } catch {
      // Keep auth/bootstrap routes renderable even if API is temporarily unavailable.
      return { onboardingCompleted: true, tenantSlug: null };
    }
  }

  return { fetchStatus };
}
