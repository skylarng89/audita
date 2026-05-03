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
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            New Password
          </label>
          <input
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

        <div class="mb-6">
          <label
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Confirm New Password
          </label>
          <input
            v-model="form.confirm"
            type="password"
            autocomplete="new-password"
            placeholder="••••••••••••"
            class="input"
            required
          />
          <p v-if="confirmError" class="mt-1 text-xs text-danger">
            {{ confirmError }}
          </p>
        </div>

        <button
          type="submit"
          class="btn-primary btn-lg w-full"
          :disabled="isLoading"
        >
          {{ isLoading ? "Resetting…" : "Reset Password →" }}
        </button>
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

const route = useRoute();
const { resetPassword } = useAuth();

const form = reactive({ password: "", confirm: "" });
const error = ref("");
const confirmError = ref("");
const done = ref(false);
const isLoading = ref(false);

const token = computed(() => route.query.token as string | undefined);

const strengthScore = computed(() => {
  const p = form.password;
  if (!p) return 0;
  let score = 0;
  if (p.length >= 8) score++;
  if (p.length >= 12) score++;
  if (/[A-Z]/.test(p) && /[0-9]/.test(p)) score++;
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
    const err = e as { data?: { detail?: string } };
    error.value =
      err?.data?.detail ??
      "Reset link is invalid or has expired. Please request a new one.";
  } finally {
    isLoading.value = false;
  }
}
</script>
