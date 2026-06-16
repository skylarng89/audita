<template>
  <SharedLoadingOverlay />
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
const route = useRoute();
const { show, hide } = useLoadingOverlay();

let showTimer: ReturnType<typeof setTimeout> | null = null;

watch(
  () => route.fullPath,
  () => {
    showTimer = setTimeout(() => show(), 200);
  },
);

onMounted(() => {
  document.getElementById("app-loader")?.remove();
  clearTimeout(showTimer ?? undefined);
  hide();
});
</script>
