import { useEffect, useCallback, useState, useRef } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { StoryForm } from '../form/StoryForm'
import { useStoryModal } from '../../lib/StoryModalContext'
import { useLocale } from '../../lib/LocaleContext'
import { useAuth } from '../../lib/AuthContext'
import { streamStory } from '../../lib/sseClient'
import { useActiveStory } from '../../lib/ActiveStoryContext'
import type { GenerationRequest, ModerationErrorCode } from '../../lib/types'
import styles from './StoryModal.module.css'

type Phase = 'form' | 'creating'

export function StoryModal() {
  const { open, closeModal } = useStoryModal()
  const { t } = useLocale()
  const navigate = useNavigate()
  const { user, resendVerification, refresh } = useAuth()
  const { setActiveStoryId } = useActiveStory()
  const MODERATION_CODES: readonly ModerationErrorCode[] = ['BLOCKED_INPUT', 'JUDGE_UNAVAILABLE']
  const needsVerify = !!user && !user.emailVerified
  const [resendDone, setResendDone] = useState(false)
  const [phase, setPhase] = useState<Phase>('form')
  const [error, setError] = useState<string | null>(null)
  // Active SSE controller — stored in a ref so React StrictMode's effect
  // double-invocation can't accidentally abort an in-flight stream.
  const activeCtrlRef = useRef<AbortController | null>(null)

  useEffect(() => {
    if (open) {
      setPhase('form')
      setError(null)
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && phase !== 'creating') closeModal()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, phase, closeModal])

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  // Cleanup on actual component unmount: abort any in-flight SSE.
  // Empty dep array → runs ONCE on real unmount, not on StrictMode dance.
  useEffect(() => {
    return () => {
      activeCtrlRef.current?.abort()
      activeCtrlRef.current = null
    }
  }, [])

  // SSE is triggered IMPERATIVELY from handleSubmit (not from a useEffect)
  // so React StrictMode's double-effect-invocation can't kill the stream
  // between create-and-cleanup cycles. The AbortController is parked in a
  // ref for real-unmount cleanup.
  const handleSubmit = useCallback((req: GenerationRequest) => {
    // If a previous SSE is still alive (e.g., user re-submitted somehow), abort it.
    activeCtrlRef.current?.abort()
    const ctrl = new AbortController()
    activeCtrlRef.current = ctrl

    setError(null)
    setPhase('creating')

    streamStory(
      req,
      {
        onMeta: ({ id }) => {
          if (ctrl.signal.aborted) return
          // Story row exists — route the user to the story page immediately so they
          // can watch content + panels fill in there. The SSE keeps streaming via
          // the controller stored in activeCtrlRef.
          setActiveStoryId(id)
          closeModal()
          navigate(`/stories/${id}`)
        },
        onToken: () => {},
        onDone: () => {
          if (ctrl.signal.aborted) return
          // Text streaming finished + content saved. The SERVER now kicks off the comic
          // build automatically (StoryService.generateInternal), so the client does not
          // trigger it — a second trigger would race the server's build and fail on the
          // story_panels unique key.
          activeCtrlRef.current = null
        },
        onError: ({ code, category, message }) => {
          if (ctrl.signal.aborted) return
          if (code && (MODERATION_CODES as readonly string[]).includes(code)) {
            const perCategory = code === 'BLOCKED_INPUT' && category
              ? t.moderation.byCategory?.[category]
              : undefined
            setError(perCategory ?? t.moderation[code as ModerationErrorCode])
            refresh()
          } else {
            setError(message ?? null)
          }
          setPhase('form')
          activeCtrlRef.current = null
        },
      },
      ctrl.signal,
    ).catch(err => {
      if (ctrl.signal.aborted || err?.name === 'AbortError') return
      setError(String(err))
      setPhase('form')
      activeCtrlRef.current = null
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [setActiveStoryId, closeModal, navigate, refresh, t])

  if (!open) return null

  const panelClass = phase === 'creating'
    ? `${styles.panel} ${styles.panelSquare}`
    : styles.panel

  return createPortal(
    <div
      className={styles.backdrop}
      onClick={phase !== 'creating' ? closeModal : undefined}
      role="dialog"
      aria-modal="true"
      aria-label="Створити казку"
    >
      <div className={panelClass} onClick={(e) => e.stopPropagation()}>
        {phase !== 'creating' && (
          <>
            <div className={styles.topBorder} />
            <div className={styles.header}>
              <div className={styles.ornament} aria-hidden="true">
                <svg viewBox="0 0 140 20" fill="none">
                  <path d="M10 10 Q35 3 70 10 Q105 17 130 10" stroke="currentColor" strokeWidth="1.5"/>
                  <circle cx="70" cy="10" r="4" fill="currentColor"/>
                  <path d="M67 10 L70 4 L73 10 L70 7Z" fill="currentColor" opacity="0.7"/>
                  <circle cx="35" cy="8" r="2" fill="currentColor" opacity="0.5"/>
                  <circle cx="105" cy="12" r="2" fill="currentColor" opacity="0.5"/>
                </svg>
              </div>
              <button className={styles.closeBtn} onClick={closeModal} aria-label="Закрити">✕</button>
            </div>
          </>
        )}
        <div className={styles.body}>
          {error && <div className={styles.error}>{error}</div>}
          {phase === 'form' && needsVerify && (
            <div className={styles.verifyPanel}>
              <h2 className={styles.creatingTitle}>{t.auth.messages.verifyPanelTitle}</h2>
              <p className={styles.creatingHint}>{t.auth.messages.verifyPanelBody(user!.email)}</p>
              <button
                className={styles.resendBtn}
                disabled={resendDone}
                onClick={async () => { await resendVerification(); setResendDone(true) }}
              >
                {resendDone ? '✓' : t.auth.actions.resend}
              </button>
            </div>
          )}
          {phase === 'form' && !needsVerify && (
            <StoryForm onSubmit={handleSubmit} loading={phase !== 'form'} inModal />
          )}
          {phase === 'creating' && (
            <div className={styles.creating}>
              <div className={styles.sun} aria-hidden="true">
                <svg viewBox="0 0 64 64" width="64" height="64">
                  <g className={styles.sunRays} stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                    <line x1="32" y1="4"  x2="32" y2="12" />
                    <line x1="32" y1="52" x2="32" y2="60" />
                    <line x1="4"  y1="32" x2="12" y2="32" />
                    <line x1="52" y1="32" x2="60" y2="32" />
                    <line x1="12" y1="12" x2="17.5" y2="17.5" />
                    <line x1="46.5" y1="46.5" x2="52" y2="52" />
                    <line x1="52" y1="12" x2="46.5" y2="17.5" />
                    <line x1="17.5" y1="46.5" x2="12" y2="52" />
                  </g>
                  <circle cx="32" cy="32" r="11" fill="currentColor" />
                </svg>
              </div>
              <h2 className={styles.creatingTitle}>{t.form.generating}</h2>
              <p className={styles.creatingHint}>{t.form.generatingHint}</p>
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body,
  )
}
