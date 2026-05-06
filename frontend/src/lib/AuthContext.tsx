import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { auth as authApi } from './apiClient'
import type { User } from './types'

interface AuthCtx {
  user: User | null
  loading: boolean
  signIn: (email: string, password: string) => Promise<void>
  signUp: (email: string, password: string, displayName: string) => Promise<void>
  signOut: () => Promise<void>
  requestPasswordReset: (email: string) => Promise<void>
  confirmPasswordReset: (token: string, newPassword: string) => Promise<void>
  resendVerification: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthCtx = createContext<AuthCtx | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    const env = await authApi.me()
    setUser(env?.user ?? null)
  }, [])

  useEffect(() => {
    refresh().finally(() => setLoading(false))
  }, [refresh])

  const signIn = useCallback(async (email: string, password: string) => {
    const env = await authApi.login(email, password)
    setUser(env.user)
  }, [])

  const signUp = useCallback(async (email: string, password: string, displayName: string) => {
    const env = await authApi.signup(email, password, displayName)
    setUser(env.user)
  }, [])

  const signOut = useCallback(async () => {
    await authApi.logout()
    setUser(null)
  }, [])

  const value: AuthCtx = {
    user, loading, signIn, signUp, signOut, refresh,
    requestPasswordReset: authApi.passwordResetRequest,
    confirmPasswordReset: authApi.passwordResetConfirm,
    resendVerification: authApi.resendVerification,
  }

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>
}

export function useAuth(): AuthCtx {
  const ctx = useContext(AuthCtx)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
