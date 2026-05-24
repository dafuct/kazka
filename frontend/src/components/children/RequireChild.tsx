import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../lib/AuthContext'
import { useChildren } from '../../lib/ChildrenContext'

export function RequireChild({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const { children: profiles, loading } = useChildren()
  const location = useLocation()

  if (!user || loading) return <>{children}</>
  if (profiles.length === 0 && !location.pathname.startsWith('/settings/children')) {
    return <Navigate to="/settings/children/new" replace state={{ reason: 'no-child' }} />
  }
  return <>{children}</>
}
