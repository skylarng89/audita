const overlayVisible = ref(false);
let showId = 0;
let safetyTimer: ReturnType<typeof setTimeout> | null = null;

function clearSafetyTimer() {
  if (safetyTimer != null) {
    clearTimeout(safetyTimer);
    safetyTimer = null;
  }
}

export function useLoadingOverlay() {
  function triggerShow() {
    const id = ++showId;
    setTimeout(() => {
      if (showId === id) {
        overlayVisible.value = true;
      }
    }, 200);
  }

  function show() {
    overlayVisible.value = true;
    clearSafetyTimer();
  }

  function hide() {
    showId++;
    overlayVisible.value = false;
    clearSafetyTimer();
  }

  return {
    overlayVisible: readonly(overlayVisible),
    triggerShow,
    show,
    hide,
  };
}
