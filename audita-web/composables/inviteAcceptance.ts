import type { OnboardingStatusResponse } from "~/composables/useOnboarding";
import {
  resolveInviteTenantSlug,
  resolveInviteToken,
} from "~/composables/inviteTenantResolution";

interface SubmitInviteAcceptanceInput {
  password: string;
  confirmPassword: string;
  routeToken: unknown;
  routeTenant: unknown;
  authTenantSlug: string | null;
  acceptInvite: (
    token: string,
    password: string,
    tenantSlug: string,
  ) => Promise<unknown>;
  fetchStatus: () => Promise<OnboardingStatusResponse | null>;
  setTenantSlug: (tenantSlug: string) => void;
  resolveApiErrorMessage: (error: unknown, fallback: string) => string;
}

interface SubmitInviteAcceptanceResult {
  done: boolean;
  error: string;
}

export interface InviteAcceptanceFormState {
  password: string;
  confirmPassword: string;
  done: boolean;
  error: string;
  isLoading: boolean;
}

interface RunInviteAcceptanceSubmitInput {
  state: InviteAcceptanceFormState;
  routeToken: unknown;
  routeTenant: unknown;
  authTenantSlug: string | null;
  dependencies: Omit<
    SubmitInviteAcceptanceInput,
    "password" | "confirmPassword" | "routeToken" | "routeTenant" | "authTenantSlug"
  >;
  submitInviteAcceptanceHandler?: (
    input: SubmitInviteAcceptanceInput,
  ) => Promise<SubmitInviteAcceptanceResult>;
}

export async function submitInviteAcceptance({
  password,
  confirmPassword,
  routeToken,
  routeTenant,
  authTenantSlug,
  acceptInvite,
  fetchStatus,
  setTenantSlug,
  resolveApiErrorMessage,
}: SubmitInviteAcceptanceInput): Promise<SubmitInviteAcceptanceResult> {
  if (password !== confirmPassword) {
    return { done: false, error: "Passwords do not match." };
  }

  if (password.length < 8) {
    return { done: false, error: "Password must be at least 8 characters." };
  }

  const token = resolveInviteToken(routeToken);
  const status = await fetchStatus();
  const tenantSlug = resolveInviteTenantSlug({
    statusTenantSlug: status?.tenantSlug,
    authTenantSlug,
    queryTenant: routeTenant,
  });

  if (status?.tenantSlug) {
    setTenantSlug(status.tenantSlug);
  }

  if (!token || !tenantSlug) {
    return { done: false, error: "Invalid invite link." };
  }

  try {
    await acceptInvite(token, password, tenantSlug);
    return { done: true, error: "" };
  } catch (error) {
    return {
      done: false,
      error: resolveApiErrorMessage(
        error,
        "This invite link is invalid or has expired.",
      ),
    };
  }
}

export async function runInviteAcceptanceSubmit({
  state,
  routeToken,
  routeTenant,
  authTenantSlug,
  dependencies,
  submitInviteAcceptanceHandler = submitInviteAcceptance,
}: RunInviteAcceptanceSubmitInput) {
  state.error = "";
  state.isLoading = true;

  try {
    const result = await submitInviteAcceptanceHandler({
      password: state.password,
      confirmPassword: state.confirmPassword,
      routeToken,
      routeTenant,
      authTenantSlug,
      ...dependencies,
    });

    state.done = result.done;
    state.error = result.error;
  } finally {
    state.isLoading = false;
  }
}
