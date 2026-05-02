type Listener = () => void

const listeners = new Set<Listener>()
let timer: ReturnType<typeof setInterval> | null = null
let activeCount = 0

const DEFAULT_INTERVAL_MS = 4000

function ensureTimer(intervalMs: number) {
  if (timer) return
  timer = setInterval(() => {
    listeners.forEach((l) => l())
  }, intervalMs)
}

function maybeStopTimer() {
  if (activeCount === 0 && timer) {
    clearInterval(timer)
    timer = null
  }
}

export function subscribeCarouselTick(listener: Listener, intervalMs = DEFAULT_INTERVAL_MS): () => void {
  listeners.add(listener)
  activeCount++
  ensureTimer(intervalMs)
  return () => {
    listeners.delete(listener)
    activeCount = Math.max(0, activeCount - 1)
    maybeStopTimer()
  }
}
