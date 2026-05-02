import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { IllustrationFrame } from '../components/story/IllustrationFrame'
import { ConfirmModal } from '../components/modal/ConfirmModal'
import { useLocale } from '../lib/LocaleContext'
import { api } from '../lib/apiClient'
import type { Story } from '../lib/types'
import styles from './StoryDetailPage.module.css'

export function StoryDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t } = useLocale()

  const [story, setStory] = useState<Story | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const [editTitle, setEditTitle] = useState('')
  const [editContent, setEditContent] = useState('')
  const [saving, setSaving] = useState(false)
  const [illustrating, setIllustrating] = useState(false)
  const [showDelete, setShowDelete] = useState(false)

  useEffect(() => {
    if (!id) return
    api.getStory(id)
      .then(s => {
        setStory(s)
        setEditTitle(s.title)
        setEditContent(s.content)
      })
      .catch(() => setError(t.errors.loadFailed))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (!story || story.illustrationStatus !== 'PENDING') return
    const poll = setInterval(async () => {
      try {
        const updated = await api.getStory(story.id)
        setStory(updated)
      } catch {
        clearInterval(poll)
      }
    }, 3000)
    return () => clearInterval(poll)
  }, [story?.illustrationStatus, story?.id])

  const handleIllustrate = useCallback(async () => {
    if (!story) return
    setIllustrating(true)
    await api.illustrate(story.id).catch(() => null)
    setStory(prev => prev ? { ...prev, illustrationStatus: 'PENDING' } : prev)
    setIllustrating(false)
  }, [story])

  const handleSave = useCallback(async () => {
    if (!story) return
    setSaving(true)
    try {
      const updated = await api.updateStory(story.id, { title: editTitle, content: editContent })
      setStory(updated)
      setEditing(false)
    } catch {
      setError(t.errors.saveFailed)
    } finally {
      setSaving(false)
    }
  }, [story, editTitle, editContent])

  const handleDelete = useCallback(async () => {
    if (!story) return
    await api.deleteStory(story.id).catch(() => null)
    navigate('/stories')
  }, [story, navigate])

  if (loading) return <div className={styles.state}>...</div>
  if (error || !story) return <div className={styles.state}>{error ?? t.errors.loadFailed}</div>

  return (
    <div className={styles.page}>
      <div className={styles.inner}>
        <Link to="/stories" className={styles.back}>← {t.story.back}</Link>

        <div className={styles.layout}>
          <div className={styles.main}>
            {editing ? (
              <input
                className={styles.titleInput}
                value={editTitle}
                onChange={e => setEditTitle(e.target.value)}
              />
            ) : (
              <h1 className={styles.title}>{story.title}</h1>
            )}

            <div className={styles.meta}>
              <span className={styles.tag}>{story.ageGroup}</span>
              <span className={styles.tag}>{story.length}</span>
              <span className={styles.tag}>{story.language.toUpperCase()}</span>
            </div>

            {editing ? (
              <textarea
                className={styles.contentInput}
                value={editContent}
                onChange={e => setEditContent(e.target.value)}
                rows={20}
              />
            ) : (
              <p className={styles.content}>{story.content}</p>
            )}

            <div className={styles.actions}>
              {editing ? (
                <>
                  <button className={styles.saveBtn} onClick={handleSave} disabled={saving}>
                    {t.story.save}
                  </button>
                  <button className={styles.cancelBtn} onClick={() => setEditing(false)}>
                    {t.story.cancel}
                  </button>
                </>
              ) : (
                <>
                  <button className={styles.editBtn} onClick={() => setEditing(true)}>
                    {t.story.edit}
                  </button>
                  <button className={styles.deleteBtn} onClick={() => setShowDelete(true)}>
                    {t.story.delete}
                  </button>
                </>
              )}
            </div>
          </div>

          <aside className={styles.aside}>
            <IllustrationFrame
              pathLight={story.illustrationPathLight}
              pathDark={story.illustrationPathDark}
              status={story.illustrationStatus}
            />
            {story.illustrationStatus !== 'PENDING' && (
              <button
                className={styles.illustrateBtn}
                onClick={handleIllustrate}
                disabled={illustrating}
              >
                {illustrating ? t.story.illustrating : t.story.illustrate}
              </button>
            )}
          </aside>
        </div>
      </div>

      {showDelete && (
        <ConfirmModal
          title={t.archive.deleteConfirm}
          text={t.archive.deleteConfirmText}
          confirmLabel={t.archive.confirmDelete}
          cancelLabel={t.archive.cancelDelete}
          onConfirm={handleDelete}
          onCancel={() => setShowDelete(false)}
        />
      )}
    </div>
  )
}
