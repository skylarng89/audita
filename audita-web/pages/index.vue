<script setup lang="ts">
// Root redirect — send authenticated users to their default page
const auth = useAuthStore()

if (auth.isAuthenticated) {
  if (auth.isSuperAdmin) await navigateTo('/platform')
  else await navigateTo('/dashboard')
} else {
  await navigateTo('/auth/sign-in')
}
</script>
