import { createContext, useCallback, useContext, useState } from 'react'
import type { ReactNode } from 'react'

export type AuthTab = 'signIn' | 'signUp' | 'forgot'

interface Ctx {
  open: boolean
  tab: AuthTab
  openAuth: (tab?: AuthTab) => void
  closeAuth: () => void
  setTab: (tab: AuthTab) => void
}

const AuthModalCtx = createContext<Ctx | null>(null)

export function AuthModalProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  const [tab, setTab] = useState<AuthTab>('signIn')

  const openAuth = useCallback((next: AuthTab = 'signIn') => {
    setTab(next)
    setOpen(true)
  }, [])

  const closeAuth = useCallback(() => setOpen(false), [])

  return (
    <AuthModalCtx.Provider value={{ open, tab, openAuth, closeAuth, setTab }}>
      {children}
    </AuthModalCtx.Provider>
  )
}

export function useAuthModal(): Ctx {
  const ctx = useContext(AuthModalCtx)
  if (!ctx) throw new Error('useAuthModal must be used within AuthModalProvider')
  return ctx
}
