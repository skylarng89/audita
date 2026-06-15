const overlayVisible = ref(false);
let safetyTimer: ReturnType<typeof setTimeout> | null = null;

function clearSafetyTimer() {
  if (safetyTimer != null) {
    clearTimeout(safetyTimer);
    safetyTimer = null;
  }
}

export function useLoadingOverlay() {
  function show() {
    overlayVisible.value = true;
    clearSafetyTimer();
  }

  function hide() {
    overlayVisible.value = false;
    clearSafetyTimer();
  }

  return {
    overlayVisible: readonly(overlayVisible),
    show,
    hide,
  };
}
