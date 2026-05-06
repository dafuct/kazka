import type { ReactNode } from 'react'
import { useAuth } from '../../lib/AuthContext'

export function RequireAdmin({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()

  if (loading) return <p style={{ padding: 32, textAlign: 'center' }}>...</p>
  if (!user || user.role !== 'ADMIN') {
    return <p style={{ padding: 32, textAlign: 'center' }}>404</p>
  }
  return <>{children}</>
}
