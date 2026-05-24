<template>
  <div>
    <!-- Logo -->
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

    <!-- Step indicator -->
    <div class="flex items-center gap-2 mb-8">
      <div
        class="flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold transition-colors"
        :class="
          step === 1 ? 'bg-primary text-white' : 'bg-primary/10 text-primary'
        "
      >
        1
      </div>
      <div class="flex-1 h-px bg-outline-variant" />
      <div
        class="flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold transition-colors"
        :class="
          step === 2 ? 'bg-primary text-white' : 'bg-primary/10 text-primary'
        "
      >
        2
      </div>
    </div>

    <!-- Step 1: Organisation -->
    <div v-if="step === 1">
      <h1 class="text-2xl font-bold text-on-surface mb-1">
        Set up your organisation
      </h1>
      <p class="text-sm text-muted mb-8">
        This is the name your team will see across the platform.
      </p>

      <div
        v-if="error"
        class="mb-4 rounded-lg bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
      >
        {{ error }}
      </div>

      <form @submit.prevent="goToStep2" novalidate class="space-y-5">
        <div>
          <label class="field-label" for="setup-org-name"
            >Organisation Name</label
          >
          <input
            id="setup-org-name"
            v-model="form.orgName"
            @input="suggestSlug"
            type="text"
            class="input"
            placeholder="Acme Corp"
            required
            autofocus
          />
        </div>

        <div>
          <label class="field-label" for="setup-slug">
            Slug
            <span
              class="normal-case tracking-normal text-xs font-medium text-muted"
            >
              (used in URLs — lowercase, hyphens only)
            </span>
          </label>
          <input
            id="setup-slug"
            v-model="form.slug"
            type="text"
            class="input font-mono"
            placeholder="acme-corp"
            required
            pattern="[a-z0-9\-]+"
          />
        </div>

        <button type="submit" class="btn-primary btn-lg w-full !mt-6">
          Continue →
        </button>
      </form>
    </div>

    <!-- Step 2: Admin account -->
    <div v-else>
      <h1 class="text-2xl font-bold text-on-surface mb-1">
        Create your admin account
      </h1>
      <p class="text-sm text-muted mb-8">
        You'll be the administrator of <strong>{{ form.orgName }}</strong
        >.
      </p>

      <div
        v-if="error"
        class="mb-4 rounded-lg bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
      >
        {{ error }}
      </div>

      <form @submit.prevent="handleSubmit" novalidate class="space-y-5">
        <div>
          <label class="field-label" for="setup-full-name">Full Name</label>
          <input
            id="setup-full-name"
            v-model="form.fullName"
            type="text"
            autocomplete="name"
            class="input"
            placeholder="Alex Thompson"
            required
          />
        </div>

        <div>
          <label class="field-label" for="setup-email">Email Address</label>
          <input
            id="setup-email"
            v-model="form.email"
            type="email"
            autocomplete="email"
            class="input"
            placeholder="admin@acme.com"
            required
          />
        </div>

        <div>
          <label class="field-label" for="setup-password">Password</label>
          <input
            id="setup-password"
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            class="input"
            placeholder="••••••••••••"
            required
            minlength="12"
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
            <span class="text-muted/70">
              — min 12 chars, upper, lower, number, symbol</span
            >
          </p>
        </div>

        <div class="flex gap-3 !mt-6">
          <button
            type="button"
            class="btn-secondary btn-lg flex-1"
            @click="step = 1"
          >
            ← Back
          </button>
          <button
            type="submit"
            class="btn-primary btn-lg flex-1"
            :disabled="isLoading"
          >
            {{ isLoading ? "Setting up…" : "Launch Audita →" }}
          </button>
        </div>
      </form>
    </div>

    <p class="mt-8 text-center text-xs text-muted">
      SECURE ENVIRONMENT &bull; AES-256 ENCRYPTED
    </p>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "auth" });

const api = useApi();
const auth = useAuthStore();
const { login } = useAuth();
const { invalidateStatus } = useOnboarding();

const step = ref(1);
const form = reactive({
  orgName: "",
  slug: "",
  fullName: "",
  email: "",
  password: "",
});
const error = ref("");
const isLoading = ref(false);

function suggestSlug() {
  form.slug = form.orgName
    .toLowerCase()
    .replaceAll(/[^a-z0-9]+/g, "-")
    .replaceAll(/^-|-$/g, "");
}

function goToStep2() {
  error.value = "";
  if (!form.orgName.trim()) {
    error.value = "Organisation name is required.";
    return;
  }
  if (!form.slug.trim() || !/^[a-z0-9-]+$/.test(form.slug)) {
    error.value =
      "Slug is required and must be lowercase letters, numbers, and hyphens only.";
    return;
  }
  step.value = 2;
}

const strengthScore = computed(() => {
  const p = form.password;
  if (!p) return 0;
  let score = 0;
  if (p.length >= 12) score++;
  if (/[A-Z]/.test(p) && /\d/.test(p)) score++;
  if (/[^A-Za-z0-9]/.test(p)) score++;
  if (p.length >= 16) score++;
  return score;
});

const strengthLabel = computed(() => {
  return ["", "Weak", "Fair", "Good", "Strong"][strengthScore.value] ?? "";
});

function strengthColor(bar: number) {
  if (bar > strengthScore.value) return "bg-border dark:bg-border-dark";
  if (strengthScore.value <= 1) return "bg-danger";
  if (strengthScore.value === 2) return "bg-warning";
  if (strengthScore.value === 3) return "bg-info";
  return "bg-success";
}

async function handleSubmit() {
  if (isLoading.value) return;
  error.value = "";

  if (!form.fullName.trim()) {
    error.value = "Full name is required.";
    return;
  }
  if (!form.email.trim()) {
    error.value = "Email is required.";
    return;
  }
  if (form.password.length < 12) {
    error.value = "Password must be at least 12 characters.";
    return;
  }

  isLoading.value = true;
  try {
    await api("/api/platform/v1/setup", {
      method: "POST",
      credentials: "omit",
      body: {
        orgName: form.orgName,
        slug: form.slug,
        fullName: form.fullName,
        email: form.email,
        password: form.password,
      },
    });

    // Store tenant slug so the login API call sends X-Tenant-Slug
    auth.setTenantSlug(form.slug);

    // Bust the cached onboarding status so the next navigation check
    // fetches fresh data and sees onboardingCompleted: true.
    invalidateStatus();

    // Auto-login the new admin
    await login(form.email, form.password);
  } catch (e: unknown) {
    error.value = resolveApiErrorMessage(e, "Setup failed. Please try again.");
  } finally {
    isLoading.value = false;
  }
}
</script>
