import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { billing as billingApi } from './apiClient'
import type { Entitlement } from './types'
import { useAuth } from './AuthContext'

interface BillingCtx {
  isPro: boolean
  entitlements: Entitlement[]
  loading: boolean
  refresh: () => Promise<void>
}

const Ctx = createContext<BillingCtx | null>(null)

export function BillingProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [entitlements, setEntitlements] = useState<Entitlement[]>([])
  const [loading, setLoading] = useState(false)

  const refresh = useCallback(async () => {
    if (!user) {
      setEntitlements([])
      return
    }
    setLoading(true)
    try {
      const rows = await billingApi.entitlements()
      setEntitlements(rows)
    } catch {
      setEntitlements([])
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    refresh()
  }, [refresh])

  const isPro = entitlements.some(e => e.state === 'ACTIVE' || e.state === 'GRACE')

  return <Ctx.Provider value={{ isPro, entitlements, loading, refresh }}>{children}</Ctx.Provider>
}

export function useBilling(): BillingCtx {
  const v = useContext(Ctx)
  if (!v) throw new Error('useBilling must be used within BillingProvider')
  return v
}
