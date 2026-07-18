import { useEffect, useRef } from 'react'
import { useNarration } from '../../lib/useNarration'
import styles from './ReaderAudioBar.module.css'

interface ReaderAudioBarProps {
  storyId: string
  text: string
  lang?: string
  label: string
  stopLabel: string
  preparingLabel: string
  /** Start narration on mount (the opening click is the user gesture). */
  autoStart?: boolean
}

export function ReaderAudioBar({ storyId, text, lang = 'uk', label, stopLabel, preparingLabel, autoStart }: ReaderAudioBarProps) {
  const { phase, progress, toggle, start } = useNarration(storyId, text, lang)
  const autoStarted = useRef(false)

  useEffect(() => {
    if (autoStart && !autoStarted.current) {
      autoStarted.current = true
      start()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoStart])

  const title = phase === 'preparing' ? preparingLabel : phase === 'playing' ? stopLabel : label

  return (
    <div className={styles.bar}>
      <button
        type="button"
        className={styles.play}
        onClick={toggle}
        aria-pressed={phase !== 'idle'}
        aria-busy={phase === 'preparing'}
        aria-label={title}
      >
        {phase === 'playing' ? '❚❚' : '▶'}
      </button>
      <div className={styles.mid}>
        <div className={styles.label}>{title}</div>
        <div className={styles.track}><div className={styles.fill} style={{ width: `${progress}%` }} /></div>
      </div>
      <span className={styles.mic} aria-hidden="true">🎙️</span>
    </div>
  )
}
