import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { children as childrenApi } from './apiClient'
import type { ChildProfileDto } from './types'
import { useAuth } from './AuthContext'

interface ChildrenCtx {
  children: ChildProfileDto[]
  active: ChildProfileDto | null
  setActive: (id: string) => void
  refetch: () => Promise<void>
  loading: boolean
}

const STORAGE_KEY = 'kazka.activeChildId'

const Ctx = createContext<ChildrenCtx | null>(null)

export function ChildrenProvider({ children: kids }: { children: ReactNode }) {
  const { user } = useAuth()
  const [list, setList] = useState<ChildProfileDto[]>([])
  const [activeId, setActiveId] = useState<string | null>(() => localStorage.getItem(STORAGE_KEY))
  const [loading, setLoading] = useState(false)

  const refetch = useCallback(async () => {
    if (!user) { setList([]); return }
    setLoading(true)
    try {
      const rows = await childrenApi.list()
      setList(rows)
      const stored = localStorage.getItem(STORAGE_KEY)
      const ok = stored && rows.some(r => r.id === stored)
      if (!ok && rows.length > 0) {
        localStorage.setItem(STORAGE_KEY, rows[0].id)
        setActiveId(rows[0].id)
      } else {
        setActiveId(stored)
      }
    } catch { setList([]) }
    finally { setLoading(false) }
  }, [user])

  useEffect(() => { refetch() }, [refetch])

  const setActive = useCallback((id: string) => {
    localStorage.setItem(STORAGE_KEY, id)
    setActiveId(id)
  }, [])

  const active = list.find(c => c.id === activeId) ?? null

  return (
    <Ctx.Provider value={{ children: list, active, setActive, refetch, loading }}>
      {kids}
    </Ctx.Provider>
  )
}

export function useChildren(): ChildrenCtx {
  const v = useContext(Ctx)
  if (!v) throw new Error('useChildren must be used within ChildrenProvider')
  return v
}
