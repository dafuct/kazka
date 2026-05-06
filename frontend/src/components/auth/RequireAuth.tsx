import { useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useAuthModal } from '../../lib/AuthModalContext'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const { openAuth } = useAuthModal()

  useEffect(() => {
    if (!loading && !user) openAuth('signIn')
  }, [loading, user, openAuth])

  if (loading) return <p style={{ padding: 32, textAlign: 'center' }}>...</p>
  if (!user) return <Navigate to="/" replace />
  return <>{children}</>
}
