<script setup lang="ts">
// Root redirect — send authenticated users to their default page
const auth = useAuthStore();
const { fetchStatus } = useOnboarding();

const onboarding = await fetchStatus();

if (auth.isAuthenticated) {
  if (auth.isSuperAdmin) await navigateTo("/platform");
  else await navigateTo("/dashboard");
} else {
  // If the API is unreachable, default to sign-in (existing session scenario).
  // A fresh install will return onboardingCompleted: false from the API.
  const destination =
    onboarding !== null && !onboarding.onboardingCompleted
      ? "/setup"
      : "/auth/sign-in";
  await navigateTo(destination);
}
</script>
