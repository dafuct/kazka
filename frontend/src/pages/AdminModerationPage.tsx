import { useEffect, useState, useCallback } from 'react'
import { adminModeration } from '../lib/apiClient'
import type { FlaggedAttemptDto, SuspendedUserDto } from '../lib/apiClient'
import type { PageResponse } from '../lib/types'
import { useLocale } from '../lib/LocaleContext'
import type { Locale } from '../locales/uk'
import styles from './AdminModerationPage.module.css'

type ModerationStrings = Locale['admin']['moderation']

export function AdminModerationPage() {
  const { t } = useLocale()
  const tm = t.admin.moderation

  const [flagged, setFlagged] = useState<PageResponse<FlaggedAttemptDto> | null>(null)
  const [suspended, setSuspended] = useState<SuspendedUserDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    adminModeration.listFlagged(page, 50).then(setFlagged).catch(e => {
      console.error('adminModeration.listFlagged failed', e)
      setError(tm.loadError)
    })
    adminModeration.listSuspended().then(setSuspended).catch(e => {
      console.error('adminModeration.listSuspended failed', e)
      setError(tm.loadError)
    })
  }, [page, tm.loadError])

  useEffect(() => { load() }, [load])

  const unsuspend = useCallback(async (id: string) => {
    if (!window.confirm(tm.unsuspendConfirm)) return
    await adminModeration.unsuspend(id)
    load()
  }, [load, tm.unsuspendConfirm])

  if (error) return <p className={styles.msg}>{error}</p>
  if (!flagged || !suspended) return <p className={styles.msg}>{tm.loading}</p>

  return (
    <div className={`${styles.page} kz-page`}>
      <h1 className={styles.heading}>{tm.title}</h1>

      <SuspendedSection tm={tm} users={suspended} onUnsuspend={unsuspend} />

      <FlaggedSection tm={tm} page={flagged} onPageChange={setPage} />
    </div>
  )
}

function SuspendedSection({
  tm,
  users,
  onUnsuspend,
}: {
  tm: ModerationStrings
  users: SuspendedUserDto[]
  onUnsuspend: (id: string) => void
}) {
  return (
    <section className={styles.section}>
      <h2 className={styles.sectionHeading}>{tm.suspendedSection(users.length)}</h2>
      {users.length === 0 ? (
        <p className={styles.empty}>{tm.suspendedEmpty}</p>
      ) : (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>{tm.colEmail}</th>
              <th>{tm.colName}</th>
              <th>{tm.colSuspendedAt}</th>
              <th>{tm.colReason}</th>
              <th>{tm.colBy}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id}>
                <td>{u.email}</td>
                <td>{u.displayName}</td>
                <td>{new Date(u.suspendedAt).toLocaleString()}</td>
                <td>{u.suspendedReason}</td>
                <td>{u.suspendedBy ?? tm.autoBy}</td>
                <td>
                  <button
                    type="button"
                    className={styles.unsuspendBtn}
                    onClick={() => onUnsuspend(u.id)}
                  >
                    {tm.unsuspend}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}

function FlaggedSection({
  tm,
  page,
  onPageChange,
}: {
  tm: ModerationStrings
  page: PageResponse<FlaggedAttemptDto>
  onPageChange: (next: number | ((prev: number) => number)) => void
}) {
  const pageNum = page.page + 1
  const isLastPage = (page.page + 1) * page.size >= page.total

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionHeading}>{tm.flaggedSection(pageNum)}</h2>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>{tm.colWhen}</th>
            <th>{tm.colUser}</th>
            <th>{tm.colPipeline}</th>
            <th>{tm.colCategory}</th>
            <th>{tm.colLang}</th>
            <th>{tm.colPrompt}</th>
          </tr>
        </thead>
        <tbody>
          {page.items.map(f => (
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
        <button
          type="button"
          disabled={page.page === 0}
          onClick={() => onPageChange(p => Math.max(0, p - 1))}
        >
          {tm.prev}
        </button>
        <span>{tm.pageLabel(pageNum)}</span>
        <button
          type="button"
          disabled={isLastPage}
          onClick={() => onPageChange(p => p + 1)}
        >
          {tm.next}
        </button>
      </div>
    </section>
  )
}
