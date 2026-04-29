<script setup lang="ts">
// Root redirect — send authenticated users to their default page
const auth = useAuthStore();
const { fetchStatus } = useOnboarding();

const onboarding = await fetchStatus();

if (auth.isAuthenticated) {
  if (auth.isSuperAdmin) await navigateTo("/platform");
  else await navigateTo("/dashboard");
} else {
  const destination = onboarding.onboardingCompleted
    ? "/auth/sign-in"
    : "/setup";
  await navigateTo(destination);
}
</script>
