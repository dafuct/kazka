import { useEffect, useState } from 'react'
import { OrnamentBand } from '../components/stitch/OrnamentBand'
import { admin } from '../lib/apiClient'
import type { AdminUser } from '../lib/apiClient'
import styles from './AdminUsersPage.module.css'

export function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    admin.listUsers().then(setUsers).catch(e => {
      console.error('admin.listUsers failed', e)
      setError('Failed to load users.')
    })
  }, [])

  if (error) return <p className={styles.msg}>{error}</p>
  if (!users) return <p className={styles.msg}>...</p>

  return (
    <div className={`${styles.page} kz-page`}><OrnamentBand framed={false} stitch={5} cols={120} className="kz-orn-top" />
      <h1 className={styles.heading}>Users ({users.length})</h1>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Email</th><th>Name</th><th>Role</th>
            <th>Verified</th><th>Google</th><th>Created</th><th>Stories</th>
          </tr>
        </thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.email}</td>
              <td>{u.displayName}</td>
              <td>{u.role}</td>
              <td>{u.emailVerified ? '✓' : '✗'}</td>
              <td>{u.googleLinked ? '✓' : '✗'}</td>
              <td>{new Date(u.createdAt).toLocaleString()}</td>
              <td>{u.storyCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
