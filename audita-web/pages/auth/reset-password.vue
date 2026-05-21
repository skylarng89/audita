<template>
  <div>
    <p
      class="text-xs font-semibold uppercase tracking-[0.14em] text-primary/70 mb-2"
    >
      Recovery Access
    </p>
    <h1 class="text-3xl font-bold tracking-tight mb-1">Reset Password</h1>
    <p class="text-sm text-muted mb-8 leading-relaxed">
      Choose a strong new password for your account.
    </p>

    <div
      v-if="done"
      class="rounded-md bg-success-light border border-success px-4 py-4 text-sm text-green-800"
    >
      Password reset successfully.
      <NuxtLink to="/auth/sign-in" class="font-semibold underline ml-1"
        >Sign in</NuxtLink
      >
    </div>

    <div v-else>
      <div
        v-if="error"
        class="mb-4 rounded-md bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
      >
        {{ error }}
      </div>

      <form @submit.prevent="handleSubmit" novalidate>
        <div class="mb-4">
          <label
            for="reset-new-password"
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            New Password
          </label>
          <div class="relative">
            <input
              id="reset-new-password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              :autocomplete="showPassword ? 'off' : 'new-password'"
              placeholder="••••••••••••"
              class="input pr-10"
              required
              minlength="8"
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
          <!-- Strength bar -->
          <div class="flex gap-1 mt-1.5" aria-hidden="true">
            <div
              v-for="i in 4"
              :key="i"
              class="h-1 flex-1 rounded-full transition-colors"
              :class="strengthColor(i)"
            />
          </div>
          <!-- Screen-reader strength announcement -->
          <p
            class="text-xs text-muted mt-1"
            aria-live="polite"
            aria-atomic="true"
          >
            Password strength: {{ strengthLabel }}
          </p>
        </div>

        <div class="mb-6">
          <label
            for="reset-confirm-password"
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Confirm New Password
          </label>
          <div class="relative">
            <input
              id="reset-confirm-password"
              v-model="form.confirm"
              :type="showConfirm ? 'text' : 'password'"
              :autocomplete="showConfirm ? 'off' : 'new-password'"
              placeholder="••••••••••••"
              class="input pr-10"
              required
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-on-surface transition-colors"
              :aria-label="
                showConfirm ? 'Hide confirm password' : 'Show confirm password'
              "
              :aria-pressed="showConfirm"
              @click="showConfirm = !showConfirm"
            >
              <svg
                v-if="showConfirm"
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
          <p v-if="confirmError" class="mt-1 text-xs text-danger" role="alert">
            {{ confirmError }}
          </p>
        </div>

        <SharedAppButton
          type="submit"
          size="lg"
          class="w-full"
          :loading="isLoading"
        >
          Reset Password &rarr;
        </SharedAppButton>
      </form>
    </div>

    <div class="mt-6 text-center">
      <NuxtLink to="/auth/sign-in" class="text-sm text-primary hover:underline">
        &larr; Back to Sign In
      </NuxtLink>
    </div>

    <div
      class="mt-6 rounded-xl border border-outline-variant/50 bg-primary/5 px-4 py-3 text-xs text-on-surface-variant leading-relaxed"
    >
      <p class="font-semibold uppercase tracking-wider text-slate-600 mb-1">
        Session Security
      </p>
      Use at least 12 characters with mixed case, numbers, and symbols for
      stronger protection.
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "auth" });

useHead({ title: "Reset Password — Audita" });

const route = useRoute();
const { resetPassword } = useAuth();

const form = reactive({ password: "", confirm: "" });
const error = ref("");
const confirmError = ref("");
const done = ref(false);
const isLoading = ref(false);
const showPassword = ref(false);
const showConfirm = ref(false);

const token = computed(() => route.query.token as string | undefined);

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

async function handleSubmit() {
  error.value = "";
  confirmError.value = "";

  if (!token.value) {
    error.value = "Invalid reset link. Please request a new one.";
    return;
  }
  if (form.password.length < 8) {
    error.value = "Password must be at least 8 characters.";
    return;
  }
  if (form.password !== form.confirm) {
    confirmError.value = "Passwords do not match.";
    return;
  }

  isLoading.value = true;
  try {
    await resetPassword(token.value, form.password);
    done.value = true;
  } catch (e: unknown) {
    error.value = resolveApiErrorMessage(
      e,
      "Reset link is invalid or has expired. Please request a new one.",
    );
  } finally {
    isLoading.value = false;
  }
}
</script>
