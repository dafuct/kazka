import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { ComicsReader } from '../components/comics/ComicsReader'
import { ConfirmModal } from '../components/modal/ConfirmModal'
import { AvatarInitials } from '../components/children/AvatarInitials'
import { ExtractedCharactersPanel } from '../components/children/ExtractedCharactersPanel'
import { BranchingReader } from '../components/branching/BranchingReader'
import { LanguageToggle } from '../components/translation/LanguageToggle'
import { ReadAloud } from '../components/story/ReadAloud'
import { ScMotif, ScBand, SCM, THREAD, BAND_PALETTE } from '../components/stitch/StitchCanvas'
import { useLocale } from '../lib/LocaleContext'
import { useChildren } from '../lib/ChildrenContext'
import { useAuth } from '../lib/AuthContext'
import { api, admin } from '../lib/apiClient'
import type { Story } from '../lib/types'
import styles from './StoryDetailPage.module.css'

export function StoryDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t } = useLocale()
  const { children: childProfiles } = useChildren()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [story, setStory] = useState<Story | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const [editTitle, setEditTitle] = useState('')
  const [editContent, setEditContent] = useState('')
  const [saving, setSaving] = useState(false)
  const [showDelete, setShowDelete] = useState(false)
  const [viewLanguage, setViewLanguage] = useState<'original' | 'translated'>('original')
  const [showcaseBusy, setShowcaseBusy] = useState(false)

  const refresh = useCallback(() => {
    if (!id) return
    api.getStory(id).then(s => setStory(s)).catch(() => null)
  }, [id])

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

  // Poll while extraction is in progress (max 10 polls × 3s = 30s)
  useEffect(() => {
    if (!story) return
    const s = story.extractionStatus
    if (s !== 'PENDING' && s !== 'RUNNING') return
    let count = 0
    const interval = setInterval(() => {
      count++
      if (count >= 10) { clearInterval(interval); return }
      refresh()
    }, 3000)
    return () => clearInterval(interval)
  }, [story?.extractionStatus, story?.id])

  // Poll while illustrations are being generated (panels filling in or status still PENDING).
  useEffect(() => {
    if (!story) return
    if (story.illustrationStatus !== 'PENDING') return
    if (story.panels.length >= 1) return
    const interval = setInterval(() => refresh(), 3000)
    return () => clearInterval(interval)
  }, [story?.illustrationStatus, story?.panels.length, story?.id])

  const handleRetry = useCallback(async () => {
    if (!story) return
    await api.retry(story.id).catch(() => null)
    refresh()
  }, [story, refresh])

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
    try {
      await api.deleteStory(story.id)
      navigate('/stories')
    } catch {
      setError(t.errors.saveFailed)
      setShowDelete(false)
    }
  }, [story, navigate, t])

  const handleToggleShowcase = useCallback(async () => {
    if (!story) return
    const next = !story.showcase
    setShowcaseBusy(true)
    try {
      await admin.setShowcase(story.id, next)
      setStory(prev => (prev ? { ...prev, showcase: next } : prev))
    } catch {
      setError(t.errors.saveFailed)
    } finally {
      setShowcaseBusy(false)
    }
  }, [story, t])

  if (loading) return <div className={styles.state}>...</div>
  if (error || !story) return <div className={styles.state}>{error ?? t.errors.loadFailed}</div>

  if (story.isBranching === true && story.branchingState !== 'complete') {
    return (
      <div className={styles.page}>
        <BranchingReader story={story} onComplete={refresh} />
      </div>
    )
  }

  const childProfile = story.childProfileId
    ? childProfiles.find(p => p.id === story.childProfileId) ?? null
    : null

  const displayedContent = viewLanguage === 'translated' && story.translatedContent
    ? story.translatedContent
    : story.content

  const displayedTitle = story.title
  const readLanguage = viewLanguage === 'translated'
    ? (story.translatedLanguage ?? story.language)
    : story.language

  const paragraphs = displayedContent
    .split(/\n\s*\n+/)
    .map(p => p.trim())
    .filter(Boolean)

  return (
    <div className={styles.page}>
      <div className={styles.inner}>
        <div className={styles.topBar}>
          <Link to="/stories" className={styles.back}>← {t.story.back}</Link>
          <span className={styles.topMotif} aria-hidden="true">
            <ScMotif rule={SCM.star8} n={11} stitch={4.5} palette={THREAD} ground={null} />
          </span>
          {!(story.isBranching && story.branchingState !== 'complete') && (
            <LanguageToggle
              story={story}
              active={viewLanguage}
              onSwitch={(active, updated) => {
                setViewLanguage(active)
                if (updated) setStory(updated)
              }}
            />
          )}
        </div>

        <div className={styles.layout}>
          {/* ── Reading column ── */}
          <div className={styles.readingCol}>
            {editing ? (
              <input
                className={styles.titleInput}
                value={editTitle}
                onChange={e => setEditTitle(e.target.value)}
              />
            ) : (
              <h1 className={styles.title}>{displayedTitle}</h1>
            )}

            {childProfile && (
              <p className={styles.forChild}>
                <AvatarInitials name={childProfile.name} seed={childProfile.avatarSeed} size={18} />
                <span>{(t as any).children?.forChild ? (t as any).children.forChild(childProfile.name) : `for ${childProfile.name}`}</span>
              </p>
            )}

            <div className={styles.titleBand} aria-hidden="true">
              <ScBand cols={100} H={13} P={20} D={6} stitch={6} palette={BAND_PALETTE} ground={null} />
            </div>

            <div className={`${styles.comicsBlock} kz-illustration`}>
              <ComicsReader story={story} onRetry={handleRetry} />
            </div>

            {editing ? (
              <textarea
                className={styles.contentInput}
                value={editContent}
                onChange={e => setEditContent(e.target.value)}
                rows={20}
              />
            ) : (
              <div className={styles.content}>
                {paragraphs.map((para, i) => (
                  <p key={i} className={i === 0 ? styles.firstPara : undefined}>{para}</p>
                ))}
              </div>
            )}
          </div>

          {/* ── Sidebar ── */}
          <aside className={styles.aside}>
            <div className={styles.asideMotif} aria-hidden="true">
              <ScMotif rule={SCM.rose} n={17} stitch={6.5} palette={THREAD} ground="var(--color-surface)" />
            </div>

            {!editing && paragraphs.length > 0 && (
              <ReadAloud
                storyId={story.id}
                text={paragraphs.join('\n\n')}
                lang={readLanguage}
                label={(t.story as any).readAloud ?? 'Читати вголос'}
                stopLabel={(t.story as any).readAloudStop ?? 'Stop'}
                preparingLabel={(t.story as any).readAloudPreparing ?? 'Preparing…'}
                narrator={(t.story as any).narrator}
              />
            )}

            <div className={styles.meta}>
              <span className={styles.tag}>{story.ageGroup}</span>
              <span className={styles.tag}>{story.length}</span>
              <span className={styles.tag}>{story.language.toUpperCase()}</span>
            </div>

            {story.childProfileId && story.extractionStatus !== 'SKIPPED' && (
              <ExtractedCharactersPanel
                storyId={story.id}
                childProfileId={story.childProfileId}
                extractionStatus={story.extractionStatus as any}
                language={viewLanguage === 'translated' ? (story.translatedLanguage ?? undefined) : story.language}
                onConfirmed={() => refresh()}
              />
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
                  {isAdmin && (
                    <button
                      className={styles.editBtn}
                      onClick={handleToggleShowcase}
                      disabled={showcaseBusy}
                    >
                      {story.showcase ? t.story.removeFromExamples : t.story.featureInExamples}
                    </button>
                  )}
                  <button className={styles.deleteBtn} onClick={() => setShowDelete(true)}>
                    {t.story.delete}
                  </button>
                </>
              )}
            </div>
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
