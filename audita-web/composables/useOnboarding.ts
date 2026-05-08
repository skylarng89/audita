export interface OnboardingStatusResponse {
  onboardingCompleted: boolean;
  tenantSlug?: string | null;
}

export function useOnboarding() {
  const api = useApi();

  async function fetchStatus(): Promise<OnboardingStatusResponse | null> {
    try {
      return await api<OnboardingStatusResponse>(
        "/api/platform/v1/bootstrap/status",
        {
          method: "GET",
          credentials: "omit",
        },
      );
    } catch {
      // Return null so callers can decide the appropriate fallback for their context.
      return null;
    }
  }

  return { fetchStatus };
}
