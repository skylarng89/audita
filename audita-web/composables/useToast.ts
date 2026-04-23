type ToastType = 'success' | 'error' | 'info' | 'warning'

interface Toast {
  id: number
  message: string
  type: ToastType
  duration: number
}

const toasts = ref<Toast[]>([])
let nextId = 0

export function useToast() {
  function show(message: string, type: ToastType = 'info', duration = 4000) {
    const id = nextId++
    toasts.value.push({ id, message, type, duration })
    setTimeout(() => dismiss(id), duration)
  }

  function dismiss(id: number) {
    const idx = toasts.value.findIndex(t => t.id === id)
    if (idx !== -1) toasts.value.splice(idx, 1)
  }

  const success = (msg: string) => show(msg, 'success')
  const error   = (msg: string) => show(msg, 'error', 6000)
  const info    = (msg: string) => show(msg, 'info')
  const warning = (msg: string) => show(msg, 'warning')

  return { toasts: readonly(toasts), show, dismiss, success, error, info, warning }
}
