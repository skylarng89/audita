<template>
  <div>
    <p
      class="text-xs font-semibold uppercase tracking-[0.14em] text-primary/70 mb-2"
    >
      Security Protocol
    </p>
    <h1 class="text-3xl font-bold tracking-tight mb-1">Forgot Password?</h1>
    <p class="text-sm text-muted mb-8 leading-relaxed">
      Enter your email and we'll send you a reset link if an account exists.
    </p>

    <div
      v-if="sent"
      class="rounded-md bg-success-light border border-success px-4 py-4 text-sm text-green-800"
    >
      If that email is registered, a reset link has been sent. Check your inbox.
    </div>

    <form v-else @submit.prevent="handleSubmit" novalidate>
      <div class="mb-6">
        <label
          class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
        >
          Email Address
        </label>
        <input
          v-model="email"
          type="email"
          placeholder="you@company.com"
          class="input"
          required
        />
      </div>

      <button
        type="submit"
        class="btn-primary btn-lg w-full"
        :disabled="isLoading"
      >
        {{ isLoading ? "Sending…" : "Send Reset Link" }}
      </button>
    </form>

    <div class="mt-6 text-center">
      <NuxtLink to="/auth/sign-in" class="text-sm text-primary hover:underline">
        &larr; Back to Sign In
      </NuxtLink>
    </div>

    <div
      class="mt-6 rounded-xl border border-outline-variant/50 bg-primary/5 px-4 py-3 text-xs text-on-surface-variant leading-relaxed"
    >
      <p class="font-semibold uppercase tracking-wider text-slate-600 mb-1">
        Identity Check
      </p>
      For security reasons, we do not confirm whether an email exists in our
      system.
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "auth" });

useHead({ title: "Forgot Password — Audita" });

const { forgotPassword } = useAuth();
const email = ref("");
const sent = ref(false);
const isLoading = ref(false);

async function handleSubmit() {
  if (!email.value) return;
  isLoading.value = true;
  try {
    await forgotPassword(email.value);
    sent.value = true;
  } finally {
    isLoading.value = false;
  }
}
</script>
