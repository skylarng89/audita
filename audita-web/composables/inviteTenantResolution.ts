interface ResolveInviteTenantSlugInput {
  statusTenantSlug: string | null | undefined;
  authTenantSlug: string | null;
  queryTenant: unknown;
}

export function normalizeInviteQueryValue(value: unknown) {
  if (typeof value !== "string") {
    return null;
  }

  const normalized = value.trim();
  if (!normalized) {
    return null;
  }

  return normalized;
}

export function resolveInviteToken(queryToken: unknown) {
  return normalizeInviteQueryValue(queryToken);
}

export function resolveInviteTenantSlug({
  statusTenantSlug,
  authTenantSlug,
  queryTenant,
}: ResolveInviteTenantSlugInput) {
  if (statusTenantSlug) {
    return statusTenantSlug;
  }

  if (authTenantSlug) {
    return authTenantSlug;
  }

  return normalizeInviteQueryValue(queryTenant);
}
