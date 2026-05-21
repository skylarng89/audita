<template>
  <div>
    <h1 class="text-2xl font-bold text-on-surface dark:text-gray-100 mb-1">
      Sign In
    </h1>
    <p class="text-sm text-muted mb-8">
      Welcome back. Please enter your credentials to continue.
    </p>

    <div
      v-if="showSetupSuccess"
      class="mb-4 rounded-lg bg-success-light border border-success/30 px-4 py-3 text-sm text-green-800"
    >
      Organisation set up successfully. Sign in to continue.
    </div>

    <div
      v-if="errors.general"
      class="mb-4 rounded-lg bg-danger-light border border-danger/20 px-4 py-3 text-sm text-danger"
      role="alert"
      aria-live="assertive"
    >
      {{ errors.general }}
    </div>

    <form @submit.prevent="handleSubmit" novalidate>
      <!-- Email -->
      <div class="mb-4">
        <label
          for="sign-in-email"
          class="block text-[11px] font-semibold uppercase tracking-[0.12em] text-muted mb-1.5"
        >
          Corporate Email
        </label>
        <div class="relative">
          <input
            id="sign-in-email"
            v-model="form.email"
            type="email"
            autocomplete="email"
            placeholder="admin@audita.io"
            class="input pr-10"
            :class="{ 'input-error': errors.email }"
            required
          />
          <span
            class="absolute right-3.5 top-1/2 -translate-y-1/2 text-muted/50 pointer-events-none"
          >
            <svg
              class="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
                d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
              />
            </svg>
          </span>
        </div>
        <p v-if="errors.email" class="mt-1 text-xs text-danger">
          {{ errors.email }}
        </p>
      </div>

      <!-- Password -->
      <div class="mb-6">
        <div class="flex items-center justify-between mb-1.5">
          <label
            for="sign-in-password"
            class="text-[11px] font-semibold uppercase tracking-[0.12em] text-muted"
            >Security Key</label
          >
          <NuxtLink
            to="/auth/forgot-password"
            class="text-xs font-semibold text-primary hover:underline uppercase tracking-wide"
          >
            Forgot Password?
          </NuxtLink>
        </div>
        <div class="relative">
          <input
            id="sign-in-password"
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            :autocomplete="showPassword ? 'off' : 'current-password'"
            placeholder="••••••••••••"
            class="input pr-10"
            :class="{ 'input-error': errors.password }"
            required
          />
          <button
            type="button"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-on-surface transition-colors"
            :aria-label="showPassword ? 'Hide password' : 'Show password'"
            :aria-pressed="showPassword"
            @click="showPassword = !showPassword"
          >
            <svg
              v-if="showPassword"
              class="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
                d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"
              />
            </svg>
            <svg
              v-else
              class="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
                d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
              />
            </svg>
          </button>
        </div>
        <p v-if="errors.password" class="mt-1 text-xs text-danger">
          {{ errors.password }}
        </p>
      </div>

      <!-- Submit -->
      <SharedAppButton
        type="submit"
        size="lg"
        class="w-full"
        :loading="isLoading"
      >
        Sign In &rarr;
      </SharedAppButton>
    </form>

    <!-- SSO divider -->
    <div class="relative my-6">
      <div class="absolute inset-0 flex items-center">
        <div class="w-full border-t border-outline-variant" />
      </div>
      <div class="relative flex justify-center">
        <span
          class="px-3 bg-white dark:bg-slate-800 text-[10px] text-muted uppercase tracking-[0.16em]"
        >
          Or Authorized SSO
        </span>
      </div>
    </div>

    <!-- SSO buttons -->
    <div class="grid grid-cols-2 gap-3">
      <a
        :href="`/api/v1/auth/oauth/google`"
        class="flex items-center justify-center gap-2.5 rounded-xl border border-outline-variant bg-white hover:bg-surface-container-low px-4 py-2.5 text-sm font-medium text-on-surface shadow-sm transition-all hover:shadow hover:border-outline"
        aria-label="Sign in with Google"
      >
        <svg class="w-4 h-4 shrink-0" viewBox="0 0 24 24">
          <path
            fill="#4285F4"
            d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
          />
          <path
            fill="#34A853"
            d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
          />
          <path
            fill="#FBBC05"
            d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
          />
          <path
            fill="#EA4335"
            d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
          />
        </svg>
        Google
      </a>
      <a
        :href="`/api/v1/auth/oauth/microsoft`"
        class="flex items-center justify-center gap-2.5 rounded-xl border border-outline-variant bg-white hover:bg-surface-container-low px-4 py-2.5 text-sm font-medium text-on-surface shadow-sm transition-all hover:shadow hover:border-outline"
        aria-label="Sign in with Microsoft"
      >
        <svg class="w-4 h-4 shrink-0" viewBox="0 0 24 24">
          <rect x="1" y="1" width="10" height="10" fill="#F25022" />
          <rect x="13" y="1" width="10" height="10" fill="#7FBA00" />
          <rect x="1" y="13" width="10" height="10" fill="#00A4EF" />
          <rect x="13" y="13" width="10" height="10" fill="#FFB900" />
        </svg>
        Microsoft
      </a>
    </div>

    <!-- Security footer -->
    <p
      class="mt-8 text-center text-[10px] text-muted/70 uppercase tracking-widest"
    >
      Secure Environment &bull; AES-256 Encrypted &bull; SOC2 Compliant
    </p>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "auth" });

useHead({ title: "Sign In — Audita" });

const { login } = useAuth();
const auth = useAuthStore();
const route = useRoute();

const form = reactive({ email: "", password: "" });
const errors = reactive({ email: "", password: "", general: "" });
const isLoading = ref(false);
const showPassword = ref(false);
const showSetupSuccess = computed(() => route.query.setup === "done");

async function handleSubmit() {
  errors.email = "";
  errors.password = "";
  errors.general = "";

  if (!form.email) {
    errors.email = "Email is required.";
    return;
  }
  if (!form.password) {
    errors.password = "Password is required.";
    return;
  }

  isLoading.value = true;
  try {
    await login(form.email, form.password);
  } catch (e: unknown) {
    errors.general = resolveApiErrorMessage(
      e,
      "Invalid credentials. Please try again.",
    );
  } finally {
    isLoading.value = false;
  }
}
</script>
