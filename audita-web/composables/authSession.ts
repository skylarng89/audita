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

export function extractPermissionsFromToken(
  accessToken: string | null,
): string[] | null {
  if (!accessToken) {
    return null;
  }

  const parts = accessToken.split(".");
  if (parts.length < 2) {
    return null;
  }

  try {
    const payload = parts[1]!.replace(/-/g, "+").replace(/_/g, "/");
    const padded = payload.padEnd(
      payload.length + ((4 - (payload.length % 4)) % 4),
      "=",
    );
    const claims = JSON.parse(atob(padded)) as Record<string, unknown>;
    const permissions = claims.permissions;
    if (Array.isArray(permissions)) {
      return permissions.filter((p): p is string => typeof p === "string");
    }
    return null;
  } catch {
    return null;
  }
}
