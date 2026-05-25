type HeaderValue = string | string[] | undefined;
type HeaderMap = Record<string, HeaderValue>;

const ALLOWED_METHODS = new Set(["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]);
const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH"]);
const ALLOWED_CONTENT_TYPES = [
  "application/json",
  "multipart/form-data",
  "application/x-www-form-urlencoded",
  "text/plain",
];
const ALLOWED_HEADERS = new Set([
  "accept",
  "authorization",
  "content-type",
  "content-length",
  "cookie",
  "x-audita-api-contract",
  "x-forwarded-host",
  "x-idempotency-key",
  "x-setup-token",
  "x-tenant-slug",
]);

export function sanitizeProxyHeaders(headers: HeaderMap): Record<string, string> {
  const sanitized: Record<string, string> = {};
  for (const [name, raw] of Object.entries(headers)) {
    const headerName = name.toLowerCase();
    if (!ALLOWED_HEADERS.has(headerName)) {
      continue;
    }

    if (raw == null) {
      continue;
    }

    const value = Array.isArray(raw) ? raw.join(",") : raw;
    if (value.trim() === "") {
      continue;
    }
    sanitized[headerName] = value;
  }
  return sanitized;
}

export function validateProxyRequest(
  method: string,
  path: string,
  contentType: string | null,
): void {
  if (!ALLOWED_METHODS.has(method)) {
    throw new Error("Unsupported proxy method");
  }

  const loweredPath = path.toLowerCase();
  if (!path.startsWith("/api/") || loweredPath.includes("..") || loweredPath.includes("%2e%2e") || loweredPath.includes("\\")) {
    throw new Error("Unsafe proxy path");
  }

  if (!MUTATING_METHODS.has(method)) {
    return;
  }

  if (!contentType) {
    return;
  }

  const normalized = contentType.toLowerCase();
  const supported = ALLOWED_CONTENT_TYPES.some((candidate) => normalized.startsWith(candidate));
  if (!supported) {
    throw new Error("Unsupported content type");
  }
}

export function buildProxyTarget(baseUrl: string, pathname: string, search: string): string {
  const normalizedBase = baseUrl.replace(/\/+$/, "");
  return `${normalizedBase}${pathname}${search}`;
}
