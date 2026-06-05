/**
 * Composable that automatically cleans up DOM event listeners on unmount.
 * Use instead of manual addEventListener/removeEventListener pairs.
 */
export function useEventListener(
  target: EventTarget,
  event: string,
  handler: EventListener,
) {
  onMounted(() => target.addEventListener(event, handler))
  onUnmounted(() => target.removeEventListener(event, handler))
}
