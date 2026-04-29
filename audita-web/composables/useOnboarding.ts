export interface OnboardingStatusResponse {
  onboardingCompleted: boolean;
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
      return { onboardingCompleted: true };
    }
  }

  return { fetchStatus };
}
