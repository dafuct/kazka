import { useEffect, useLayoutEffect, useRef } from 'react'

interface Options {
  enabled: boolean
  intervalMs: number
  onTick: () => void
}

export function useAutoAdvance({ enabled, intervalMs, onTick }: Options): void {
  const tickRef = useRef(onTick)
  // Keep ref fresh so a new closure doesn't reset the interval
  useLayoutEffect(() => { tickRef.current = onTick })

  useEffect(() => {
    if (!enabled) return
    const id = window.setInterval(() => tickRef.current(), intervalMs)
    return () => window.clearInterval(id)
  }, [enabled, intervalMs])
}
