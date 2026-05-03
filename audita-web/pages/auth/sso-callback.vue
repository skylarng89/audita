<template>
  <!-- This page is the SSO callback landing — it exchanges a one-time code for auth tokens,
       stores auth state, then navigates to the appropriate dashboard. -->
  <div class="flex items-center justify-center min-h-screen">
    <div class="text-center">
      <div v-if="errorMessage" class="max-w-sm mx-auto">
        <div
          class="mb-4 rounded-md bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
        >
          {{ errorMessage }}
        </div>
        <NuxtLink to="/auth/sign-in" class="btn-primary btn-md">
          Back to Sign In
        </NuxtLink>
      </div>
      <div v-else class="flex flex-col items-center gap-3 text-muted">
        <!-- Spinner -->
        <svg
          class="animate-spin h-8 w-8 text-primary"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            class="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            stroke-width="4"
          />
          <path
            class="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
        <p class="text-sm">Completing sign-in…</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AuthResponse } from "~/types";

definePageMeta({ layout: false });

const route = useRoute();
const auth = useAuthStore();
const errorMessage = ref("");
const config = useRuntimeConfig();

onMounted(async () => {
  const ssoError = route.query.sso_error as string | undefined;
  if (ssoError) {
    errorMessage.value =
      "SSO sign-in failed. Please try again or use email and password.";
    return;
  }

  const hashCode = (() => {
    const rawHash = globalThis.location.hash;
    if (!rawHash || !rawHash.startsWith("#")) {
      return undefined;
    }
    const params = new URLSearchParams(rawHash.slice(1));
    return params.get("code") ?? undefined;
  })();

  const code = hashCode;

  if (!code) {
    errorMessage.value = "Incomplete SSO response. Please try again.";
    return;
  }

  try {
    const result = await $fetch<AuthResponse>("/api/v1/auth/oauth/exchange", {
      method: "POST",
      baseURL: config.public.apiBase as string,
      body: { code },
      credentials: "include",
    });

    auth.setAuth(result);

    // Clear callback URL params to avoid code reuse and reduce sensitive URL residue.
    globalThis.history.replaceState({}, document.title, "/auth/sso-callback");

    // Role-based redirect (AUTH-022)
    if (result.role === "SUPER_ADMIN") {
      await navigateTo("/platform");
    } else {
      await navigateTo("/dashboard");
    }
  } catch {
    errorMessage.value = "Your SSO session expired. Please sign in again.";
  }
});
</script>
