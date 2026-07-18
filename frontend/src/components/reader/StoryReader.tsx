import { useCallback, useEffect, useMemo, useState } from 'react'
import { paginate } from './paginate'
import { ReaderAudioBar } from './ReaderAudioBar'
import { BranchingChoiceButtons } from '../branching/BranchingChoiceButtons'
import { useLocale } from '../../lib/LocaleContext'
import type { BranchingChoice } from '../../lib/types'
import styles from './StoryReader.module.css'

interface StoryReaderProps {
  title: string
  text: string
  cover?: string | null
  onClose: () => void
  /** Present only for own stories — narration audio bar (+ optional autoplay). */
  audio?: { storyId: string; lang: string; autoPlay?: boolean }
  /** Branching: pending choices, shown on the last page. */
  choices?: BranchingChoice[]
  onPickChoice?: (choiceId: string) => void
  choiceBusy?: boolean
  choicePrompt?: string
  choiceError?: string | null
}

export function StoryReader({
  title, text, cover, onClose, audio,
  choices, onPickChoice, choiceBusy, choicePrompt, choiceError,
}: StoryReaderProps) {
  const { t } = useLocale()
  const pages = useMemo(() => paginate(text), [text])
  const total = Math.max(pages.length, 1)
  const [page, setPage] = useState(0)

  // Clamp when text grows/shrinks (branching appends fragments).
  useEffect(() => {
    setPage(p => Math.min(p, total - 1))
  }, [total])

  const go = useCallback(
    (d: number) => setPage(p => Math.max(0, Math.min(total - 1, p + d))),
    [total],
  )

  useEffect(() => {
    const key = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight') go(1)
      if (e.key === 'ArrowLeft') go(-1)
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', key)
    return () => window.removeEventListener('keydown', key)
  }, [go, onClose])

  // Lock body scroll while the overlay is open.
  useEffect(() => {
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = '' }
  }, [])

  const paras = pages[page] ?? []
  const last = page === total - 1
  const showIllus = page === 0 && !!cover

  return (
    <div className={styles.reader} role="dialog" aria-modal="true" aria-label={title}>
      <div className={styles.top}>
        <button type="button" className="icon-btn" onClick={onClose} aria-label={t.reader.close}>✕</button>
        <div className={styles.topTitle}>{title}</div>
        <div className={styles.counter}>{t.reader.pageOf(page + 1, total)}</div>
      </div>

      <div className={styles.stage}>
        <button type="button" className={page === 0 ? `${styles.arrow} ${styles.hide}` : styles.arrow} onClick={() => go(-1)} aria-label="‹">‹</button>

        <div className={showIllus ? styles.page : `${styles.page} ${styles.pageNoIllus}`} key={page}>
          {showIllus && <img className={`${styles.illus} fadein`} src={cover!} alt="" aria-hidden="true" />}
          <div className="fadein">
            {page === 0 && <div className="eyebrow" style={{ marginBottom: 10 }}>{t.reader.eyebrow}</div>}
            <div className={styles.text}>
              {paras.map((p, i) =>
                page === 0 && i === 0 ? (
                  <p key={i} className={styles.dcPara}>
                    <span className={styles.dc}>{p.charAt(0)}</span>
                    {p.slice(1)}
                  </p>
                ) : (
                  <p key={i}>{p}</p>
                ),
              )}
            </div>

            {last && choices && choices.length > 0 && onPickChoice && (
              <div className={styles.choiceWrap}>
                {choiceBusy ? (
                  <p className={styles.choiceBusy}>{t.branching.loading}</p>
                ) : (
                  <BranchingChoiceButtons
                    choices={choices}
                    onPick={onPickChoice}
                    prompt={choicePrompt ?? ''}
                  />
                )}
                {choiceError && <p className={styles.choiceError}>{choiceError}</p>}
              </div>
            )}

            {last && (!choices || choices.length === 0) && (
              <div className={styles.lastActions}>
                <button type="button" className="btn btn-soft" onClick={() => setPage(0)}>{t.reader.restart}</button>
              </div>
            )}
          </div>
        </div>

        <button type="button" className={last ? `${styles.arrow} ${styles.hide}` : styles.arrow} onClick={() => go(1)} aria-label="›">›</button>
      </div>

      <div className={styles.bottom}>
        {audio && (
          <ReaderAudioBar
            storyId={audio.storyId}
            text={text}
            lang={audio.lang}
            label={t.reader.listen}
            stopLabel={t.reader.reading}
            preparingLabel={t.reader.preparing}
            autoStart={audio.autoPlay}
          />
        )}
        <div className={styles.dots}>
          {pages.map((_, i) => (
            <button
              key={i}
              type="button"
              className={i === page ? `${styles.dot} ${styles.dotOn}` : styles.dot}
              onClick={() => setPage(i)}
              aria-label={t.reader.pageOf(i + 1, total)}
            />
          ))}
        </div>
      </div>
    </div>
  )
}
