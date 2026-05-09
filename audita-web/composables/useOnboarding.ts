export interface OnboardingStatusResponse {
  onboardingCompleted: boolean;
  tenantSlug?: string | null;
}

export function useOnboarding() {
  const api = useApi();

  // useState persists the result across navigations within the same session.
  // undefined = not yet fetched; null = last fetch failed; object = cached result.
  const cachedStatus = useState<OnboardingStatusResponse | null | undefined>(
    "onboarding-status",
    () => undefined,
  );

  async function fetchStatus(
    force = false,
  ): Promise<OnboardingStatusResponse | null> {
    if (!force && cachedStatus.value !== undefined) {
      return cachedStatus.value;
    }
    try {
      const result = await api<OnboardingStatusResponse>(
        "/api/platform/v1/bootstrap/status",
        { method: "GET", credentials: "omit" },
      );
      cachedStatus.value = result;
      return result;
    } catch {
      // Do not cache failures — allow a retry on the next call.
      return null;
    }
  }

  function invalidateStatus() {
    cachedStatus.value = undefined;
  }

  return { fetchStatus, invalidateStatus };
}
