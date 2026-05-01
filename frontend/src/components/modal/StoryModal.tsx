import { useEffect, useCallback, useState } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { StoryForm } from '../form/StoryForm'
import { StoryStream } from '../story/StoryStream'
import { useStoryModal } from '../../lib/StoryModalContext'
import { useLocale } from '../../lib/LocaleContext'
import { api } from '../../lib/apiClient'
import type { GenerationRequest } from '../../lib/types'
import styles from './StoryModal.module.css'

type Phase = 'form' | 'streaming'

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
      if (e.key === 'Escape' && phase !== 'streaming') closeModal()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, phase, closeModal])

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  const handleSubmit = useCallback((req: GenerationRequest) => {
    setRequest(req)
    setError(null)
    setPhase('streaming')
  }, [])

  const handleDone = useCallback((id: string) => {
    api.illustrate(id).catch(() => null)
    closeModal()
    navigate(`/stories/${id}`)
  }, [navigate, closeModal])

  const handleError = useCallback((message: string) => {
    setError(message)
    setPhase('form')
  }, [])

  if (!open) return null

  return createPortal(
    <div
      className={styles.backdrop}
      onClick={phase !== 'streaming' ? closeModal : undefined}
      role="dialog"
      aria-modal="true"
      aria-label="Створити казку"
    >
      <div className={styles.panel} onClick={(e) => e.stopPropagation()}>
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
          {phase !== 'streaming' && (
            <button className={styles.closeBtn} onClick={closeModal} aria-label="Закрити">✕</button>
          )}
        </div>
        <div className={styles.body}>
          {error && <div className={styles.error}>{error}</div>}
          {phase === 'form' && (
            <StoryForm onSubmit={handleSubmit} loading={false} inModal />
          )}
          {phase === 'streaming' && request && (
            <div className={styles.streaming}>
              <h2 className={styles.streamingTitle}>{t.form.generating}</h2>
              <StoryStream request={request} onDone={handleDone} onError={handleError} />
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body
  )
}
