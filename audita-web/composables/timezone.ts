const DEFAULT_TIMEZONE = "UTC";

type SettingsResponse = {
  profile?: {
    timezone?: string | null;
  };
};

let ianaTimezonesCache: string[] | null = null;

export function getIanaTimezones(): string[] {
  if (ianaTimezonesCache) {
    return ianaTimezonesCache;
  }

  const supportedValuesOf = (
    Intl as Intl.DateTimeFormatOptions & {
      supportedValuesOf?: (key: string) => string[];
    }
  ).supportedValuesOf;

  const zones =
    typeof supportedValuesOf === "function"
      ? supportedValuesOf("timeZone")
      : [DEFAULT_TIMEZONE];

  const normalized = new Set(zones);
  normalized.add(DEFAULT_TIMEZONE);
  ianaTimezonesCache = [...normalized].sort((left, right) =>
    left.localeCompare(right),
  );
  return ianaTimezonesCache;
}

export function normalizeTimezone(value: string | null | undefined): string {
  if (!value) {
    return DEFAULT_TIMEZONE;
  }

  const candidate = value.trim();
  if (!candidate) {
    return DEFAULT_TIMEZONE;
  }

  if (!getIanaTimezones().includes(candidate)) {
    return DEFAULT_TIMEZONE;
  }

  return candidate;
}

export function useTenantTimezone() {
  return useState<string>("tenant-timezone", () => DEFAULT_TIMEZONE);
}

export function setTenantTimezone(value: string | null | undefined) {
  useTenantTimezone().value = normalizeTimezone(value);
}

export async function loadTenantTimezoneFromSettings() {
  const isLoaded = useState<boolean>("tenant-timezone-loaded", () => false);
  if (isLoaded.value) {
    return;
  }

  const api = useApi();
  try {
    const response = await api<SettingsResponse>("/api/v1/settings", {
      method: "GET",
    });
    setTenantTimezone(response.profile?.timezone ?? DEFAULT_TIMEZONE);
  } catch {
    setTenantTimezone(DEFAULT_TIMEZONE);
  } finally {
    isLoaded.value = true;
  }
}

export function formatDateInTimezone(
  iso: string | null | undefined,
  timezone: string,
): string {
  if (!iso) {
    return "—";
  }
  try {
    return new Intl.DateTimeFormat("en-GB", {
      timeZone: timezone,
      year: "numeric",
      month: "short",
      day: "2-digit",
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}

export function formatDateTimeInTimezone(
  iso: string | null | undefined,
  timezone: string,
): string {
  if (!iso) {
    return "—";
  }
  try {
    return new Intl.DateTimeFormat("en-GB", {
      timeZone: timezone,
      year: "numeric",
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      timeZoneName: "short",
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}

export function formatDateInTenantTimezone(iso: string | null | undefined): string {
  return formatDateInTimezone(iso, useTenantTimezone().value);
}

export function formatDateTimeInTenantTimezone(
  iso: string | null | undefined,
): string {
  return formatDateTimeInTimezone(iso, useTenantTimezone().value);
}
