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

        <p class="field-hint">No data returned.</p>
      </section>

      <section class="card p-5 shadow-card-hover space-y-4">
        <h2 class="text-lg font-semibold">Feature Flags</h2>

        <div class="space-y-3 text-sm text-muted">
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Policy breach digests</span>
            <AppBadge variant="neutral">Unavailable</AppBadge>
          </div>
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Automated reminders</span>
            <AppBadge variant="neutral">Unavailable</AppBadge>
          </div>
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Conditional escalation</span>
            <AppBadge variant="neutral">Unavailable</AppBadge>
          </div>
        </div>

        <p class="field-hint">No data returned.</p>
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
          <p class="mt-1 text-sm font-semibold">No data returned.</p>
        </div>
        <div
          class="rounded-md border border-border p-4 dark:border-border-dark"
        >
          <p class="text-xs uppercase tracking-wide text-muted">MFA Policy</p>
          <p class="mt-1 text-sm font-semibold">No data returned.</p>
        </div>
        <div
          class="rounded-md border border-border p-4 dark:border-border-dark"
        >
          <p class="text-xs uppercase tracking-wide text-muted">
            Password Policy
          </p>
          <p class="mt-1 text-sm font-semibold">No data returned.</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "default" });

const settings = reactive({
  name: "No data returned.",
  slug: "No data returned.",
  email: "No data returned.",
  timezone: "No data returned.",
});
</script>
