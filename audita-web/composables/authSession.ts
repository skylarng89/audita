export function computeTokenExpiresAt(
  expiresInSeconds: number,
  now = Date.now(),
) {
  return now + Math.max(0, expiresInSeconds) * 1000;
}

export function hasActiveAccessToken(
  accessToken: string | null,
  tokenExpiresAt: number | null,
  now = Date.now(),
) {
  return Boolean(accessToken) && tokenExpiresAt != null && tokenExpiresAt > now;
}

interface RefreshDecision {
  alreadyRetried: boolean;
  isAuthenticated: boolean;
  isRefreshEndpoint: boolean;
  responseStatus: number;
}

export function shouldAttemptTokenRefresh({
  alreadyRetried,
  isAuthenticated,
  isRefreshEndpoint,
  responseStatus,
}: RefreshDecision) {
  return (
    isAuthenticated &&
    !isRefreshEndpoint &&
    !alreadyRetried &&
    responseStatus === 401
  );
}
