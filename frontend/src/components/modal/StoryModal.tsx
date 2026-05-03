import { useEffect, useCallback, useState } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { StoryForm } from '../form/StoryForm'
import { useStoryModal } from '../../lib/StoryModalContext'
import { useLocale } from '../../lib/LocaleContext'
import { streamStory } from '../../lib/sseClient'
import { api } from '../../lib/apiClient'
import type { GenerationRequest } from '../../lib/types'
import styles from './StoryModal.module.css'

type Phase = 'form' | 'creating'

const POLL_INTERVAL_MS = 2000
const POLL_TIMEOUT_MS = 60000

export function StoryModal() {
  const { open, closeModal } = useStoryModal()
  const { t } = useLocale()
  const navigate = useNavigate()
  const [phase, setPhase] = useState<Phase>('form')
  const [request, setRequest] = useState<GenerationRequest | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setPhase('form')
      setRequest(null)
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

  useEffect(() => {
    if (!open || phase !== 'creating' || !request) return

    const ctrl = new AbortController()
    let cancelled = false
    let pollTimer: number | undefined

    const pollUntilReady = async (id: string) => {
      const startedAt = Date.now()
      const tick = async () => {
        if (cancelled) return
        try {
          const story = await api.getStory(id)
          if (story.illustrationStatus !== 'PENDING') {
            if (!cancelled) {
              closeModal()
              navigate(`/stories/${id}`)
            }
            return
          }
        } catch {
          // network blip — keep polling until timeout
        }
        if (Date.now() - startedAt > POLL_TIMEOUT_MS) {
          if (!cancelled) {
            closeModal()
            navigate(`/stories/${id}`)
          }
          return
        }
        pollTimer = window.setTimeout(tick, POLL_INTERVAL_MS)
      }
      tick()
    }

    streamStory(
      request,
      {
        onToken: () => {},
        onDone: ({ id }) => {
          if (cancelled) return
          // Best-effort: poll loop observes the resulting illustrationStatus.
          api.illustrate(id).catch(() => null)
          pollUntilReady(id)
        },
        onError: ({ message }) => {
          if (cancelled) return
          setError(message)
          setPhase('form')
        },
      },
      ctrl.signal,
    ).catch(err => {
      if (cancelled || err?.name === 'AbortError') return
      setError(String(err))
      setPhase('form')
    })

    return () => {
      cancelled = true
      ctrl.abort()
      if (pollTimer) window.clearTimeout(pollTimer)
    }
  }, [open, phase, request, closeModal, navigate])

  const handleSubmit = useCallback((req: GenerationRequest) => {
    setRequest(req)
    setError(null)
    setPhase('creating')
  }, [])

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
          {phase === 'form' && (
            <StoryForm onSubmit={handleSubmit} loading={false} inModal />
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
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body,
  )
}
