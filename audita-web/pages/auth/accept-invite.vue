<template>
  <div>
    <div class="mb-2">
      <span class="inline-flex items-center gap-1.5 text-xs font-semibold text-primary bg-primary/10 px-2.5 py-1 rounded-full">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
        Invitation Confirmed
      </span>
    </div>
    <h1 class="text-2xl font-bold mb-1">Complete Setup</h1>
    <p class="text-sm text-muted mb-8">Please provide your details to finalise your account creation.</p>

    <div v-if="error" class="mb-4 rounded-md bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger">
      {{ error }}
    </div>

    <div v-if="done" class="rounded-md bg-success-light border border-success px-4 py-4 text-sm text-green-800">
      Account activated! <NuxtLink to="/auth/sign-in" class="font-semibold underline">Sign in</NuxtLink>
    </div>

    <form v-else @submit.prevent="handleSubmit" novalidate>
      <div class="mb-4">
        <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Full Name</label>
        <input v-model="form.fullName" type="text" placeholder="Alex Thompson" class="input" required />
      </div>

      <div class="mb-4">
        <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">New Password</label>
        <input v-model="form.password" type="password" placeholder="••••••••••••" class="input" required minlength="8" />
        <!-- Strength bar -->
        <div class="flex gap-1 mt-1.5">
          <div v-for="i in 4" :key="i" class="h-1 flex-1 rounded-full transition-colors"
               :class="strengthColor(i)" />
        </div>
        <p class="text-xs text-muted mt-1">Password strength: {{ strengthLabel }}</p>
      </div>

      <div class="mb-6">
        <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Confirm Password</label>
        <input v-model="form.confirm" type="password" placeholder="••••••••••••" class="input" required />
      </div>

      <button type="submit" class="btn-primary btn-lg w-full" :disabled="isLoading">
        {{ isLoading ? 'Setting up…' : 'Complete Setup →' }}
      </button>
    </form>

    <div class="flex justify-between mt-8 text-xs text-muted">
      <a href="#" class="hover:underline">Privacy Policy</a>
      <a href="#" class="hover:underline">Terms of Service</a>
      <span class="flex items-center gap-1">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
        AES-256 Encrypted Session
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'auth' })

const route = useRoute()
const { acceptInvite } = useAuth()

const form = reactive({ fullName: '', password: '', confirm: '' })
const error = ref('')
const isLoading = ref(false)
const done = ref(false)

const strength = computed(() => {
  const p = form.password
  if (p.length < 6) return 1
  if (p.length < 8) return 2
  if (/[A-Z]/.test(p) && /[0-9]/.test(p)) return 4
  return 3
})

const strengthLabel = computed(() =>
  ['', 'Weak', 'Moderate', 'Good', 'Strong'][strength.value]
)

function strengthColor(segment: number) {
  if (segment > strength.value) return 'bg-gray-200'
  if (strength.value <= 1) return 'bg-danger'
  if (strength.value === 2) return 'bg-warning'
  if (strength.value === 3) return 'bg-info'
  return 'bg-success'
}

async function handleSubmit() {
  error.value = ''
  if (form.password !== form.confirm) {
    error.value = 'Passwords do not match.'
    return
  }
  if (form.password.length < 8) {
    error.value = 'Password must be at least 8 characters.'
    return
  }

  const token = route.query.token as string
  if (!token) { error.value = 'Invalid invite link.'; return }

  isLoading.value = true
  try {
    await acceptInvite(token, form.fullName, form.password)
    done.value = true
  } catch {
    error.value = 'This invite link is invalid or has expired.'
  } finally {
    isLoading.value = false
  }
}
</script>
