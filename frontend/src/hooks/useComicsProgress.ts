import { useEffect, useState } from 'react'
import type { components } from '@kazka/shared'

type Status = components['schemas']['StoryStatusDto']

/**
 * Polls /api/stories/{id}/status every 3s while the tale is in progress.
 * Stops polling on READY or FAILED.
 */
export function useComicsProgress(storyId: string | null): Status | null {
  const [status, setStatus] = useState<Status | null>(null)

  useEffect(() => {
    if (!storyId) {
      setStatus(null)
      return
    }
    let cancelled = false
    let timer: number | null = null

    async function tick() {
      try {
        const res = await fetch(`/api/stories/${storyId}/status`, { credentials: 'include' })
        if (!res.ok) {
          if (!cancelled) timer = window.setTimeout(tick, 5000)
          return
        }
        const next = (await res.json()) as Status
        if (cancelled) return
        setStatus(next)
        if (next.status !== 'READY' && next.status !== 'FAILED') {
          timer = window.setTimeout(tick, 3000)
        }
      } catch {
        if (!cancelled) timer = window.setTimeout(tick, 5000)
      }
    }
    tick()

    return () => {
      cancelled = true
      if (timer) window.clearTimeout(timer)
    }
  }, [storyId])

  return status
}
