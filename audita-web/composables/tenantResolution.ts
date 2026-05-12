interface ResolveTenantSlugInput {
  hostname: string;
  queryTenant?: string | null;
  isDev: boolean;
}

const RESERVED_SUBDOMAINS = new Set(["www", "app", "api", "mail", "smtp"]);

export function resolveTenantSlug({
  hostname,
  queryTenant,
  isDev,
}: ResolveTenantSlugInput) {
  const hostWithoutPort = hostname.split(":")[0] ?? "";
  const parts = hostWithoutPort.split(".");
  const subdomain = parts.length >= 3 ? parts[0] : null;

  const slugFromSubdomain =
    subdomain && !RESERVED_SUBDOMAINS.has(subdomain) ? subdomain : null;

  if (slugFromSubdomain) {
    return slugFromSubdomain;
  }

  if (!isDev) {
    return null;
  }

  return queryTenant ?? null;
}
