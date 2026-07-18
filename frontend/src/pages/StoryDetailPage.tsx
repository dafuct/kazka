import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { ComicsReader } from '../components/comics/ComicsReader'
import { ConfirmModal } from '../components/modal/ConfirmModal'
import { AvatarInitials } from '../components/children/AvatarInitials'
import { ExtractedCharactersPanel } from '../components/children/ExtractedCharactersPanel'
import { LanguageToggle } from '../components/translation/LanguageToggle'
import { TapedCard } from '../components/taped/TapedCard'
import { StoryReader } from '../components/reader/StoryReader'
import { useLocale } from '../lib/LocaleContext'
import { useChildren } from '../lib/ChildrenContext'
import { useAuth } from '../lib/AuthContext'
import { api, admin, branching } from '../lib/apiClient'
import type { Story } from '../lib/types'
import styles from './StoryDetailPage.module.css'

export function StoryDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t } = useLocale()
  const { children: childProfiles } = useChildren()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const tb = (t as any).branching ?? {}

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
  const [reader, setReader] = useState<{ open: boolean; autoPlay: boolean }>({ open: false, autoPlay: false })
  const [choiceBusy, setChoiceBusy] = useState(false)
  const [choiceError, setChoiceError] = useState<string | null>(null)

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

  // Poll while illustrations are being generated.
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

  const pickChoice = useCallback(async (choiceId: string) => {
    if (!story) return
    setChoiceBusy(true)
    setChoiceError(null)
    try {
      const resp = await branching.choose(story.id, choiceId)
      setStory(prev => prev
        ? {
            ...prev,
            content: resp.content ?? '',
            pendingChoices: resp.isFinal ? [] : (resp.choices ?? []),
            branchingState: (resp.isFinal ? 'complete' : prev.branchingState) as Story['branchingState'],
          }
        : prev)
      if (resp.isFinal) refresh()
    } catch (e: any) {
      setChoiceError(e?.message ?? (tb.errorRetry ?? 'Something went wrong. Please refresh.'))
    } finally {
      setChoiceBusy(false)
    }
  }, [story, refresh, tb])

  if (loading) return <div className={styles.state}>...</div>
  if (error || !story) return <div className={styles.state}>{error ?? t.errors.loadFailed}</div>

  const childProfile = story.childProfileId
    ? childProfiles.find(p => p.id === story.childProfileId) ?? null
    : null

  const displayedContent = viewLanguage === 'translated' && story.translatedContent
    ? story.translatedContent
    : story.content
  const readLanguage = viewLanguage === 'translated'
    ? (story.translatedLanguage ?? story.language)
    : story.language

  const branchingActive = story.isBranching === true && story.branchingState !== 'complete'
  const pendingChoices = branchingActive ? (story.pendingChoices ?? []) : []
  const cover = story.panels?.[0]?.imageUrl ?? null
  const firstParagraph = displayedContent.split(/\n\s*\n+/).map(p => p.trim()).filter(Boolean)[0] ?? ''
  const hasIllustration = story.panels.length > 0

  return (
    <div className="wrap">
      <div className={styles.topBar}>
        <Link to="/stories" className="link-more">{t.detail.back}</Link>
        <LanguageToggle
          story={story}
          active={viewLanguage}
          onSwitch={(active, updated) => {
            setViewLanguage(active)
            if (updated) setStory(updated)
          }}
        />
      </div>

      <section className={styles.top}>
        {/* Cover */}
        <div className="fadein">
          <TapedCard rotationKey={story.id} className={styles.coverCard}>
            <ComicsReader story={story} onRetry={handleRetry} />
          </TapedCard>
        </div>

        {/* Info */}
        <div className="fadein">
          {story.theme && <div className="eyebrow">{story.theme}</div>}
          {editing ? (
            <input className={styles.titleInput} value={editTitle} onChange={e => setEditTitle(e.target.value)} />
          ) : (
            <h1 className={styles.title}>{story.title}</h1>
          )}

          {childProfile && (
            <p className={styles.forChild}>
              <AvatarInitials name={childProfile.name} seed={childProfile.avatarSeed} size={18} />
              <span>{(t as any).children?.forChild ? (t as any).children.forChild(childProfile.name) : `for ${childProfile.name}`}</span>
            </p>
          )}

          <div className={styles.meta}>
            <span className="badge">{t.form.ageGroups[story.ageGroup as '3-5' | '6-8' | '9-12']}</span>
            <span className="badge">{t.form.lengths[story.length as 'short' | 'medium' | 'long']}</span>
            <span className="badge">{t.form.languages[story.language as 'uk' | 'en'] ?? story.language.toUpperCase()}</span>
            <span className="badge">{t.detail.audio}</span>
            {hasIllustration && <span className="badge">{t.detail.illustrated}</span>}
          </div>

          {!editing && (
            <div className={styles.cta}>
              <button type="button" className="btn btn-primary btn-lg" onClick={() => setReader({ open: true, autoPlay: false })}>
                {t.detail.read}
              </button>
              <button type="button" className="btn btn-soft btn-lg" onClick={() => setReader({ open: true, autoPlay: true })}>
                {t.detail.listen}
              </button>
            </div>
          )}

          {!editing && firstParagraph && (
            <>
              <div className="field-label">{t.detail.about}</div>
              <p className={`serif ${styles.blurb}`}>{firstParagraph}</p>
            </>
          )}

          {editing && (
            <textarea
              className={styles.contentInput}
              value={editContent}
              onChange={e => setEditContent(e.target.value)}
              rows={20}
            />
          )}

          {story.childProfileId && story.extractionStatus !== 'SKIPPED' && (
            <div className={styles.panelBlock}>
              <ExtractedCharactersPanel
                storyId={story.id}
                childProfileId={story.childProfileId}
                extractionStatus={story.extractionStatus as any}
                language={viewLanguage === 'translated' ? (story.translatedLanguage ?? undefined) : story.language}
                onConfirmed={() => refresh()}
              />
            </div>
          )}

          <div className={styles.actions}>
            {editing ? (
              <>
                <button className="btn btn-primary btn-sm" onClick={handleSave} disabled={saving}>{t.story.save}</button>
                <button className="btn btn-ghost btn-sm" onClick={() => setEditing(false)}>{t.story.cancel}</button>
              </>
            ) : (
              <>
                <button className="btn btn-ghost btn-sm" onClick={() => setEditing(true)}>{t.story.edit}</button>
                {isAdmin && (
                  <button className="btn btn-ghost btn-sm" onClick={handleToggleShowcase} disabled={showcaseBusy}>
                    {story.showcase ? t.story.removeFromExamples : t.story.featureInExamples}
                  </button>
                )}
                <button className={`btn btn-ghost btn-sm ${styles.deleteBtn}`} onClick={() => setShowDelete(true)}>
                  {t.story.delete}
                </button>
              </>
            )}
          </div>
        </div>
      </section>

      {reader.open && (
        <StoryReader
          title={story.title}
          text={displayedContent}
          cover={cover}
          onClose={() => setReader({ open: false, autoPlay: false })}
          audio={{ storyId: story.id, lang: readLanguage, autoPlay: reader.autoPlay }}
          choices={pendingChoices}
          onPickChoice={pickChoice}
          choiceBusy={choiceBusy}
          choicePrompt={tb.choicePrompt ?? 'What happens next?'}
          choiceError={choiceError}
        />
      )}

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
