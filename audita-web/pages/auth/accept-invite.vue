<template>
  <div>
    <div class="mb-2">
      <span
        class="inline-flex items-center gap-1.5 text-xs font-semibold text-primary bg-primary/10 px-2.5 py-1 rounded-full"
      >
        <svg
          class="w-3 h-3"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
          />
        </svg>
        Invitation Confirmed
      </span>
    </div>
    <h1 class="text-3xl font-bold tracking-tight mb-1">Complete Setup</h1>
    <p class="text-sm text-muted mb-8 leading-relaxed">
      Set a password to activate your account.
    </p>

    <div
      v-if="error"
      class="mb-4 rounded-md bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
    >
      {{ error }}
    </div>

    <div
      v-if="done"
      class="rounded-md bg-success-light border border-success px-4 py-4 text-sm text-green-800"
    >
      Account activated!
      <NuxtLink to="/auth/sign-in" class="font-semibold underline"
        >Sign in</NuxtLink
      >
    </div>

    <form v-else @submit.prevent="handleSubmit" novalidate>
      <div class="mb-4">
        <label
          for="invite-new-password"
          class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >New Password</label
        >
        <div class="relative">
          <input
            id="invite-new-password"
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="new-password"
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
          for="invite-confirm-password"
          class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >Confirm Password</label
        >
        <div class="relative">
          <input
            id="invite-confirm-password"
            v-model="form.confirm"
            :type="showConfirm ? 'text' : 'password'"
            autocomplete="new-password"
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
      </div>

      <SharedAppButton
        type="submit"
        size="lg"
        class="w-full shadow-lg shadow-primary/20"
        :loading="isLoading"
      >
        Complete Setup &rarr;
      </SharedAppButton>
    </form>

    <div class="flex justify-between mt-8 text-xs text-muted">
      <a href="#" class="hover:underline">Privacy Policy</a>
      <a href="#" class="hover:underline">Terms of Service</a>
      <span class="flex items-center gap-1">
        <svg
          class="w-3 h-3"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
          />
        </svg>
        AES-256 Encrypted Session
      </span>
    </div>

    <div
      class="mt-6 rounded-xl border border-outline-variant/50 bg-primary/5 px-4 py-3 text-xs text-on-surface-variant leading-relaxed"
    >
      <p class="font-semibold uppercase tracking-wider text-slate-600 mb-1">
        System Node
      </p>
      Your credentials will be provisioned with tenant-scoped access once setup
      is complete.
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "auth" });

useHead({ title: "Complete Setup — Audita" });

const route = useRoute();
const { acceptInvite } = useAuth();

const form = reactive({ password: "", confirm: "" });
const error = ref("");
const isLoading = ref(false);
const done = ref(false);
const showPassword = ref(false);
const showConfirm = ref(false);

const strength = computed(() => {
  const p = form.password;
  if (p.length < 6) return 1;
  if (p.length < 8) return 2;
  if (/[A-Z]/.test(p) && /[0-9]/.test(p)) return 4;
  return 3;
});

const strengthLabel = computed(
  () => ["", "Weak", "Moderate", "Good", "Strong"][strength.value],
);

function strengthColor(segment: number) {
  if (segment > strength.value) return "bg-gray-200";
  if (strength.value <= 1) return "bg-danger";
  if (strength.value === 2) return "bg-warning";
  if (strength.value === 3) return "bg-info";
  return "bg-success";
}

async function handleSubmit() {
  error.value = "";
  if (form.password !== form.confirm) {
    error.value = "Passwords do not match.";
    return;
  }
  if (form.password.length < 8) {
    error.value = "Password must be at least 8 characters.";
    return;
  }

  const token = route.query.token as string;
  const tenantSlug = route.query.tenant as string;
  if (!token || !tenantSlug) {
    error.value = "Invalid invite link.";
    return;
  }

  isLoading.value = true;
  try {
    await acceptInvite(token, form.password, tenantSlug);
    done.value = true;
  } catch {
    error.value = "This invite link is invalid or has expired.";
  } finally {
    isLoading.value = false;
  }
}
</script>
