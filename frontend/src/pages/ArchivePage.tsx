import { useState, useEffect, useCallback } from 'react'
import { StoryCard } from '../components/story/StoryCard'
import { ConfirmModal } from '../components/modal/ConfirmModal'
import { useLocale } from '../lib/LocaleContext'
import { api } from '../lib/apiClient'
import type { Story } from '../lib/types'
import styles from './ArchivePage.module.css'

export function ArchivePage() {
  const { t } = useLocale()
  const [stories, setStories] = useState<Story[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteId, setDeleteId] = useState<string | null>(null)

  useEffect(() => {
    api.listStories()
      .then(page => setStories(page.items))
      .catch(() => setError(t.errors.loadFailed))
      .finally(() => setLoading(false))
  }, [])

  const handleDelete = useCallback(async () => {
    if (!deleteId) return
    await api.deleteStory(deleteId).catch(() => null)
    setStories(prev => prev.filter(s => s.id !== deleteId))
    setDeleteId(null)
  }, [deleteId])

  return (
    <div className={styles.page}>
      <div className={styles.inner}>
        <h1 className={styles.heading}>{t.archive.title}</h1>

        {loading && <p className={styles.msg}>...</p>}
        {error && <p className={styles.msg}>{error}</p>}
        {!loading && !error && stories.length === 0 && (
          <p className={styles.msg}>{t.archive.empty}</p>
        )}

        <ul className={styles.grid}>
          {stories.map(story => (
            <li key={story.id}>
              <StoryCard story={story} onDelete={setDeleteId} />
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
