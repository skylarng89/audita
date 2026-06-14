<template>
  <NuxtLoadingIndicator color="#2563eb" :height="3" :throttle="0" />
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
onMounted(() => {
  document.getElementById("app-loader")?.remove()
})
</script>
