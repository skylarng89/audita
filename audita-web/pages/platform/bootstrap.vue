<template>
  <div>
    <!-- Logo / brand mark -->
    <div class="flex justify-center mb-8">
      <img
        src="/brand/audita-icon-light.svg"
        alt="Audita"
        class="w-14 h-14 rounded-2xl dark:hidden"
      />
      <img
        src="/brand/audita-icon-dark.svg"
        alt="Audita"
        class="w-14 h-14 rounded-2xl hidden dark:block"
      />
    </div>

    <div
      v-if="done"
      class="rounded-md bg-success-light border border-success px-4 py-4 text-sm text-green-800 text-center"
    >
      <p class="font-semibold mb-1">Platform bootstrapped successfully.</p>
      <p>
        You can now
        <NuxtLink to="/auth/sign-in" class="underline">sign in</NuxtLink> as
        Super Admin.
      </p>
    </div>

    <div v-else>
      <h1 class="text-2xl font-bold text-center mb-1">Platform Setup</h1>
      <p class="text-sm text-muted text-center mb-8">
        Create your Super Admin account to initialise the Audita platform. This
        page is only accessible on first run.
      </p>

      <div
        v-if="error"
        class="mb-4 rounded-md bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
      >
        {{ error }}
      </div>

      <form @submit.prevent="handleSubmit" novalidate class="space-y-4">
        <div>
          <label
            for="bootstrap-full-name"
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Full Name
          </label>
          <input
            id="bootstrap-full-name"
            v-model="form.fullName"
            type="text"
            autocomplete="name"
            placeholder="Alex Thompson"
            class="input"
            required
          />
        </div>

        <div>
          <label
            for="bootstrap-email"
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Email Address
          </label>
          <input
            id="bootstrap-email"
            v-model="form.email"
            type="email"
            autocomplete="email"
            placeholder="admin@audita.io"
            class="input"
            required
          />
        </div>

        <div>
          <label
            for="bootstrap-password"
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Password
          </label>
          <input
            id="bootstrap-password"
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            placeholder="••••••••••••"
            class="input"
            required
            minlength="8"
          />
          <!-- Strength bar -->
          <div class="flex gap-1 mt-1.5">
            <div
              v-for="i in 4"
              :key="i"
              class="h-1 flex-1 rounded-full transition-colors"
              :class="strengthColor(i)"
            />
          </div>
          <p class="text-xs text-muted mt-1">
            Password strength: {{ strengthLabel }}
          </p>
        </div>

        <button
          type="submit"
          class="btn-primary btn-lg w-full !mt-6"
          :disabled="isLoading"
        >
          {{ isLoading ? "Setting up…" : "Initialise Platform →" }}
        </button>
      </form>
    </div>

    <p class="mt-8 text-center text-xs text-muted">
      SECURE ENVIRONMENT &bull; AES-256 ENCRYPTED &bull; SOC2 COMPLIANT
    </p>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "auth" });

const api = useApi();
const { fetchStatus, invalidateStatus } = useOnboarding();

const form = reactive({ fullName: "", email: "", password: "" });
const error = ref("");
const done = ref(false);
const isLoading = ref(false);

const strengthScore = computed(() => {
  const p = form.password;
  if (!p) return 0;
  let score = 0;
  if (p.length >= 8) score++;
  if (p.length >= 12) score++;
  if (/[A-Z]/.test(p) && /\d/.test(p)) score++;
  if (/[^A-Za-z0-9]/.test(p)) score++;
  return score;
});

const strengthLabel = computed(() => {
  const labels = ["", "Weak", "Fair", "Good", "Strong"];
  return labels[strengthScore.value] ?? "";
});

function strengthColor(bar: number) {
  if (bar > strengthScore.value) return "bg-border dark:bg-border-dark";
  if (strengthScore.value <= 1) return "bg-danger";
  if (strengthScore.value === 2) return "bg-warning";
  if (strengthScore.value === 3) return "bg-info";
  return "bg-success";
}

function validateForm(): string | null {
  if (!form.fullName.trim()) return "Full name is required.";
  if (!form.email.trim()) return "Email is required.";
  if (form.password.length < 8)
    return "Password must be at least 8 characters.";
  return null;
}

function isAlreadyBootstrappedError(err: {
  status?: number;
  statusCode?: number;
  response?: { status?: number };
  data?: { detail?: string; title?: string; errorCode?: string };
}): boolean {
  const statusCode = err?.status ?? err?.statusCode ?? err?.response?.status;
  const detail = err?.data?.detail ?? "";
  const title = err?.data?.title ?? "";
  return (
    statusCode === 403 &&
    (err?.data?.errorCode === "ALREADY_BOOTSTRAPPED" ||
      /already\s+been\s+bootstrapped/i.test(detail) ||
      /already\s+bootstrapped/i.test(detail) ||
      /already\s+bootstrapped/i.test(title))
  );
}

async function redirectIfOnboardingComplete(): Promise<boolean> {
  const status = await fetchStatus(true);
  if (status?.onboardingCompleted) {
    done.value = true;
    invalidateStatus();
    await navigateTo("/auth/sign-in?setup=done");
    return true;
  }
  return false;
}

async function handleSubmit() {
  if (isLoading.value) return;
  error.value = "";

  // Guard stale tabs: onboarding might have completed in another tab/session.
  if (await redirectIfOnboardingComplete()) return;

  const validationError = validateForm();
  if (validationError) {
    error.value = validationError;
    return;
  }

  isLoading.value = true;
  try {
    await api("/api/platform/v1/bootstrap", {
      method: "POST",
      credentials: "omit",
      body: {
        fullName: form.fullName,
        email: form.email,
        password: form.password,
      },
    });
    done.value = true;
    invalidateStatus();
    await navigateTo("/auth/sign-in?setup=done");
  } catch (e: unknown) {
    const err = e as {
      status?: number;
      statusCode?: number;
      response?: { status?: number };
      data?: { detail?: string; title?: string; errorCode?: string };
    };

    // Double-submit or race: another session bootstrapped first.
    if (
      isAlreadyBootstrappedError(err) &&
      (await redirectIfOnboardingComplete())
    )
      return;

    // Final fallback: redirect if onboarding completed via race.
    if (await redirectIfOnboardingComplete()) return;

    error.value = resolveApiErrorMessage(
      err,
      "Setup failed. The platform may already be initialised.",
    );
  } finally {
    isLoading.value = false;
  }
}
</script>
