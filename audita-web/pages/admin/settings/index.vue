<template>
  <div class="space-y-6">
    <div class="flex items-start justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Administration
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Organization Settings</h1>
        <p class="text-sm text-muted mt-1">
          Configure tenant profile, governance defaults, and notification
          policy.
        </p>
      </div>
      <button type="button" class="btn-primary btn-sm" disabled>
        Save Changes
      </button>
    </div>

    <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <section class="card p-5 shadow-card-hover lg:col-span-2 space-y-5">
        <h2 class="text-lg font-semibold">Organization Profile</h2>

        <div v-if="pending" class="field-hint">
          Loading organization profile...
        </div>

        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label class="field-label" for="org-name">Organization Name</label>
            <input
              id="org-name"
              type="text"
              class="input"
              :value="settings.name"
              disabled
            />
          </div>
          <div>
            <label class="field-label" for="org-slug">Organization Slug</label>
            <input
              id="org-slug"
              type="text"
              class="input"
              :value="settings.slug"
              disabled
            />
          </div>
          <div>
            <label class="field-label" for="org-email"
              >Primary Contact Email</label
            >
            <input
              id="org-email"
              type="email"
              class="input"
              :value="settings.email"
              disabled
            />
          </div>
          <div>
            <label class="field-label" for="org-timezone">Time Zone</label>
            <input
              id="org-timezone"
              type="text"
              class="input"
              :value="settings.timezone"
              disabled
            />
          </div>
        </div>

        <p v-if="errorMessage" class="field-hint text-danger">
          {{ errorMessage }}
        </p>
      </section>

      <section class="card p-5 shadow-card-hover space-y-4">
        <h2 class="text-lg font-semibold">Feature Flags</h2>

        <div class="space-y-3 text-sm text-muted">
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Policy breach digests</span>
            <AppBadge
              :variant="
                settings.featureFlags.policyBreachDigests
                  ? 'success'
                  : 'neutral'
              "
            >
              {{
                settings.featureFlags.policyBreachDigests
                  ? "Enabled"
                  : "Disabled"
              }}
            </AppBadge>
          </div>
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Automated reminders</span>
            <AppBadge
              :variant="
                settings.featureFlags.automatedReminders ? 'success' : 'neutral'
              "
            >
              {{
                settings.featureFlags.automatedReminders
                  ? "Enabled"
                  : "Disabled"
              }}
            </AppBadge>
          </div>
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Conditional escalation</span>
            <AppBadge
              :variant="
                settings.featureFlags.conditionalEscalation
                  ? 'success'
                  : 'neutral'
              "
            >
              {{
                settings.featureFlags.conditionalEscalation
                  ? "Enabled"
                  : "Disabled"
              }}
            </AppBadge>
          </div>
        </div>

        <p v-if="errorMessage" class="field-hint text-danger">
          {{ errorMessage }}
        </p>
      </section>
    </div>

    <section class="card p-5 shadow-card-hover">
      <h2 class="text-lg font-semibold">Security Defaults</h2>
      <div class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
        <div
          class="rounded-md border border-border p-4 dark:border-border-dark"
        >
          <p class="text-xs uppercase tracking-wide text-muted">
            Session Timeout
          </p>
          <p class="mt-1 text-sm font-semibold">
            {{ settings.securityDefaults.sessionTimeoutLabel }}
          </p>
        </div>
        <div
          class="rounded-md border border-border p-4 dark:border-border-dark"
        >
          <p class="text-xs uppercase tracking-wide text-muted">MFA Policy</p>
          <p class="mt-1 text-sm font-semibold">
            {{ settings.securityDefaults.mfaPolicy }}
          </p>
        </div>
        <div
          class="rounded-md border border-border p-4 dark:border-border-dark"
        >
          <p class="text-xs uppercase tracking-wide text-muted">
            Password Policy
          </p>
          <p class="mt-1 text-sm font-semibold">
            {{ settings.securityDefaults.passwordPolicy }}
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "default" });

const api = useApi();
const { error: toastError } = useToast();

interface TenantAdminSettingsResponse {
  profile: {
    name: string;
    slug: string;
    primaryContactEmail: string | null;
    timezone: string;
    status: string;
  };
  featureFlags: {
    policyBreachDigests: boolean;
    automatedReminders: boolean;
    conditionalEscalation: boolean;
  };
  securityDefaults: {
    sessionTimeoutMinutes: number | null;
    mfaPolicy: string;
    passwordPolicy: string;
  };
}

const pending = ref(false);
const errorMessage = ref("");

const settings = reactive({
  name: "",
  slug: "",
  email: "Not configured",
  timezone: "UTC",
  featureFlags: {
    policyBreachDigests: false,
    automatedReminders: false,
    conditionalEscalation: false,
  },
  securityDefaults: {
    sessionTimeoutLabel: "Not configured",
    mfaPolicy: "Not configured",
    passwordPolicy: "Not configured",
  },
});

async function loadSettings() {
  pending.value = true;
  errorMessage.value = "";
  try {
    const response = await api<TenantAdminSettingsResponse>(
      "/api/v1/settings",
      {
        method: "GET",
      },
    );
    settings.name = response.profile.name;
    settings.slug = response.profile.slug;
    settings.email = response.profile.primaryContactEmail ?? "Not configured";
    settings.timezone = response.profile.timezone || "UTC";
    settings.featureFlags = response.featureFlags;
    settings.securityDefaults.sessionTimeoutLabel = response.securityDefaults
      .sessionTimeoutMinutes
      ? `${response.securityDefaults.sessionTimeoutMinutes} minutes`
      : "Not configured";
    settings.securityDefaults.mfaPolicy = response.securityDefaults.mfaPolicy;
    settings.securityDefaults.passwordPolicy =
      response.securityDefaults.passwordPolicy;
  } catch {
    errorMessage.value = "Unable to load settings right now.";
    toastError("Failed to load organization settings.");
  } finally {
    pending.value = false;
  }
}

onMounted(loadSettings);
</script>
