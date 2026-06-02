import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'

interface Ctx {
  activeStoryId: string | null
  setActiveStoryId: (id: string | null) => void
}

const ActiveStoryCtx = createContext<Ctx | null>(null)
const STORAGE_KEY = 'kazka.activeStoryId'

export function ActiveStoryProvider({ children }: { children: ReactNode }) {
  const [activeStoryId, setActiveStoryIdState] = useState<string | null>(() => {
    try { return window.localStorage.getItem(STORAGE_KEY) } catch { return null }
  })

  const setActiveStoryId = useCallback((id: string | null) => {
    setActiveStoryIdState(id)
    try {
      if (id) window.localStorage.setItem(STORAGE_KEY, id)
      else window.localStorage.removeItem(STORAGE_KEY)
    } catch { /* storage unavailable; non-fatal */ }
  }, [])

  // Cross-tab sync — if another tab clears or sets the id, follow it.
  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === STORAGE_KEY) setActiveStoryIdState(e.newValue)
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  return (
    <ActiveStoryCtx.Provider value={{ activeStoryId, setActiveStoryId }}>
      {children}
    </ActiveStoryCtx.Provider>
  )
}

export function useActiveStory() {
  const ctx = useContext(ActiveStoryCtx)
  if (!ctx) throw new Error('useActiveStory must be used within ActiveStoryProvider')
  return ctx
}
