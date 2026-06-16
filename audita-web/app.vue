<template>
  <SharedLoadingOverlay />
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
const route = useRoute();
const { triggerShow, hide } = useLoadingOverlay();

watch(
  () => route.fullPath,
  () => triggerShow(),
);

onMounted(() => {
  document.getElementById("app-loader")?.remove();
  hide();
});
</script>
