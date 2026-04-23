<template>
  <div>
    <h1 class="text-2xl font-bold mb-1">Forgot Password</h1>
    <p class="text-sm text-muted mb-8">
      Enter your email and we'll send you a reset link if an account exists.
    </p>

    <div v-if="sent" class="rounded-md bg-success-light border border-success px-4 py-4 text-sm text-green-800">
      If that email is registered, a reset link has been sent. Check your inbox.
    </div>

    <form v-else @submit.prevent="handleSubmit" novalidate>
      <div class="mb-6">
        <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">
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

      <button type="submit" class="btn-primary btn-lg w-full" :disabled="isLoading">
        {{ isLoading ? 'Sending…' : 'Send Reset Link' }}
      </button>
    </form>

    <div class="mt-6 text-center">
      <NuxtLink to="/auth/sign-in" class="text-sm text-primary hover:underline">
        &larr; Back to Sign In
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'auth' })

const { forgotPassword } = useAuth()
const email = ref('')
const sent = ref(false)
const isLoading = ref(false)

async function handleSubmit() {
  if (!email.value) return
  isLoading.value = true
  try {
    await forgotPassword(email.value)
    sent.value = true
  } finally {
    isLoading.value = false
  }
}
</script>
