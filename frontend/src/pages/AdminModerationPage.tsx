import { useEffect, useState, useCallback } from 'react'
import { adminModeration } from '../lib/apiClient'
import type { FlaggedAttemptDto, SuspendedUserDto } from '../lib/apiClient'
import type { PageResponse } from '../lib/types'
import styles from './AdminModerationPage.module.css'

export function AdminModerationPage() {
  const [flagged, setFlagged] = useState<PageResponse<FlaggedAttemptDto> | null>(null)
  const [suspended, setSuspended] = useState<SuspendedUserDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    adminModeration.listFlagged(page, 50).then(setFlagged).catch(e => setError(String(e)))
    adminModeration.listSuspended().then(setSuspended).catch(e => setError(String(e)))
  }, [page])

  useEffect(() => { load() }, [load])

  async function unsuspend(id: string) {
    if (!window.confirm('Unsuspend this account?')) return
    await adminModeration.unsuspend(id)
    load()
  }

  if (error) return <p className={styles.msg}>{error}</p>
  if (!flagged || !suspended) return <p className={styles.msg}>Loading…</p>

  return (
    <div className={styles.page}>
      <h1 className={styles.heading}>Moderation</h1>

      <section className={styles.section}>
        <h2 className={styles.sectionHeading}>Suspended users ({suspended.length})</h2>
        {suspended.length === 0 ? <p>None.</p> : (
          <table className={styles.table}>
            <thead>
              <tr><th>Email</th><th>Name</th><th>Suspended</th><th>Reason</th><th>By</th><th></th></tr>
            </thead>
            <tbody>
              {suspended.map(u => (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>{u.displayName}</td>
                  <td>{new Date(u.suspendedAt).toLocaleString()}</td>
                  <td>{u.suspendedReason}</td>
                  <td>{u.suspendedBy ?? 'auto'}</td>
                  <td>
                    <button type="button" className={styles.unsuspendBtn} onClick={() => unsuspend(u.id)}>
                      Unsuspend
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionHeading}>Recent flagged attempts (page {flagged.page + 1})</h2>
        <table className={styles.table}>
          <thead>
            <tr><th>When</th><th>User</th><th>Pipeline</th><th>Category</th><th>Lang</th><th>Prompt</th></tr>
          </thead>
          <tbody>
            {flagged.items.map(f => (
              <tr key={f.id}>
                <td>{new Date(f.createdAt).toLocaleString()}</td>
                <td>{f.userEmail}</td>
                <td>{f.pipeline}</td>
                <td>{f.category}</td>
                <td>{f.language}</td>
                <td className={styles.prompt}>{f.promptText}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className={styles.pager}>
          <button type="button" disabled={page === 0} onClick={() => setPage(p => Math.max(0, p - 1))}>Prev</button>
          <span>Page {flagged.page + 1}</span>
          <button type="button" disabled={(flagged.page + 1) * flagged.size >= flagged.total} onClick={() => setPage(p => p + 1)}>Next</button>
        </div>
      </section>
    </div>
  )
}
