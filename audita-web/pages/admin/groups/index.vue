<script setup lang="ts">
definePageMeta({ middleware: ["auth", "admin-only"] })

const router = useRouter()
onMounted(() => {
  router.replace("/groups")
})
</script>

<template>
  <div class="flex items-center justify-center py-20">
    <p class="text-sm text-muted">Redirecting…</p>
  </div>
</template>
