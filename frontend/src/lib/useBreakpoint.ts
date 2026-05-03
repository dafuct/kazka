import { useEffect, useState } from 'react'

/**
 * Returns true while the viewport width is at most `maxPx` (inclusive).
 * Updates on resize. SSR-safe (defaults to false).
 */
export function useBreakpoint(maxPx: number): boolean {
  const [match, setMatch] = useState<boolean>(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return false
    return window.matchMedia(`(max-width: ${maxPx}px)`).matches
  })

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return
    const mql = window.matchMedia(`(max-width: ${maxPx}px)`)
    const handler = (e: MediaQueryListEvent) => setMatch(e.matches)
    mql.addEventListener('change', handler)
    return () => mql.removeEventListener('change', handler)
  }, [maxPx])

  return match
}
