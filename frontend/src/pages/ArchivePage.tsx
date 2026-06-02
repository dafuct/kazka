import { useState, useEffect, useCallback } from 'react'
import { StoryCard } from '../components/story/StoryCard'
import { ConfirmModal } from '../components/modal/ConfirmModal'
import { useLocale } from '../lib/LocaleContext'
import { useChildren } from '../lib/ChildrenContext'
import { api } from '../lib/apiClient'
import type { Story } from '../lib/types'
import styles from './ArchivePage.module.css'

export function ArchivePage() {
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const { children: profiles } = useChildren()
  const [filterId, setFilterId] = useState<string>('all')
  const [stories, setStories] = useState<Story[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteId, setDeleteId] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    const param = filterId === 'all' ? undefined : filterId
    api.listStories(0, 20, param)
      .then(page => setStories(page.items))
      .catch(() => setError(t.errors.loadFailed))
      .finally(() => setLoading(false))
  }, [filterId])

  const handleDelete = useCallback(async () => {
    if (!deleteId) return
    try {
      await api.deleteStory(deleteId)
      setStories(prev => prev.filter(s => s.id !== deleteId))
    } catch {
      setError(t.errors.saveFailed)
    } finally {
      setDeleteId(null)
    }
  }, [deleteId])

  return (
    <div className={styles.page}>
      <div className={styles.inner}>
        <div className={styles.pageHeader}>
          <div className={styles.label}>{t.archive.label}</div>
          <h1 className={styles.heading}>{t.archive.title}</h1>
        </div>

        <div className={styles.chipStrip}>
          <button onClick={() => setFilterId('all')} aria-pressed={filterId === 'all'}
                  className={filterId === 'all' ? styles.chipOn : styles.chip}>
            {tc.filterAll ?? 'All'}
          </button>
          {profiles.map(p => (
            <button key={p.id} onClick={() => setFilterId(p.id)} aria-pressed={filterId === p.id}
                    className={filterId === p.id ? styles.chipOn : styles.chip}>
              {p.name}
            </button>
          ))}
          <button onClick={() => setFilterId('none')} aria-pressed={filterId === 'none'}
                  className={filterId === 'none' ? styles.chipOn : styles.chip}>
            {tc.filterNoChild ?? 'No child'}
          </button>
        </div>

        {loading && <p className={styles.msg}>...</p>}
        {error && <p className={styles.msg}>{error}</p>}
        {!loading && !error && stories.length === 0 && (
          <p className={styles.msg}>{t.archive.empty}</p>
        )}

        <ul className={styles.grid}>
          {stories.map(story => (
            <li key={story.id}>
              <StoryCard
                story={story}
                onDelete={setDeleteId}
                badge={
                  story.isBranching && story.branchingState !== 'complete' ? (
                    <span className={styles.continueBadge}>▸ {(t as any).branching?.continueAffordance ?? 'Continue tale'}</span>
                  ) : undefined
                }
              />
            </li>
          ))}
        </ul>
      </div>

      {deleteId && (
        <ConfirmModal
          title={t.archive.deleteConfirm}
          text={t.archive.deleteConfirmText}
          confirmLabel={t.archive.confirmDelete}
          cancelLabel={t.archive.cancelDelete}
          onConfirm={handleDelete}
          onCancel={() => setDeleteId(null)}
        />
      )}
    </div>
  )
}
