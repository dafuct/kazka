import { useEffect, useRef } from 'react'

interface Options {
  enabled: boolean
  intervalMs: number
  onTick: () => void
}

/**
 * Calls `onTick` every `intervalMs` while `enabled` is true.
 * Timer resets when `enabled` or `intervalMs` changes; passing a fresh
 * `onTick` closure each render does NOT reset it.
 */
export function useAutoAdvance({ enabled, intervalMs, onTick }: Options): void {
  const tickRef = useRef(onTick)
  useEffect(() => { tickRef.current = onTick })

  useEffect(() => {
    if (!enabled) return
    if (typeof window === 'undefined') return
    const id = window.setInterval(() => tickRef.current(), intervalMs)
    return () => window.clearInterval(id)
  }, [enabled, intervalMs])
}
