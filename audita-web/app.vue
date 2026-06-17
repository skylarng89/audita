<template>
  <SharedLoadingOverlay />
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
const route = useRoute();
const router = useRouter();
const { triggerShow, hide } = useLoadingOverlay();

watch(
  () => route.fullPath,
  () => triggerShow(),
);

router.afterEach((to) => {
  if (to.path.startsWith('/auth/') || to.path === '/setup') {
    hide();
  }
});

onMounted(() => {
  document.getElementById("app-loader")?.remove();
  hide();
});
</script>
