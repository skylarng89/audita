export interface OnboardingStatusResponse {
  onboardingCompleted: boolean;
}

export function useOnboarding() {
  const api = useApi();

  async function fetchStatus() {
    return api<OnboardingStatusResponse>("/api/platform/v1/bootstrap/status", {
      method: "GET",
    });
  }

  return { fetchStatus };
}
