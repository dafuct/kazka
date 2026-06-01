import { useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useComicsProgress } from '../../hooks/useComicsProgress'
import styles from './ProgressWidget.module.css'

const BARS = ['▱▱▱▱', '▰▱▱▱', '▰▰▱▱', '▰▰▰▱', '▰▰▰▰'] as const

export interface ProgressWidgetProps {
  /** Story currently in PENDING (or null to hide the widget). Owned by the app shell. */
  storyId: string | null
  /** Called once the widget self-dismisses (READY timeout or manual hide). */
  onClear?: () => void
}

export function ProgressWidget({ storyId, onClear }: ProgressWidgetProps) {
  const status = useComicsProgress(storyId)
  const navigate = useNavigate()
  const [dismissed, setDismissed] = useState(false)

  // Reset dismissed flag whenever a new story is being tracked.
  useEffect(() => {
    setDismissed(false)
  }, [storyId])

  useEffect(() => {
    if (status?.status === 'READY') {
      const t = window.setTimeout(() => {
        setDismissed(true)
        onClear?.()
      }, 8000)
      return () => window.clearTimeout(t)
    }
  }, [status?.status, onClear])

  if (!storyId || !status || dismissed) return null

  const phaseText = (() => {
    switch (status.status) {
      case 'WRITING':         return 'Пишемо казку…'
      case 'EXTRACTING_ACTS': return 'Розбиваємо на сцени…'
      case 'DRAWING':         return 'Малюємо комікс…'
      case 'READY':           return '✓ Казка готова →'
      case 'FAILED':          return 'Не вдалося згенерувати'
    }
  })()

  const klass =
    status.status === 'READY'  ? `${styles.widget} ${styles.ready}` :
    status.status === 'FAILED' ? `${styles.widget} ${styles.failed}` :
    styles.widget

  const barIdx = status.status === 'DRAWING' ? 2 : Math.max(0, Math.min(4, status.panelsReady))

  return (
    <button className={klass} onClick={() => navigate(`/stories/${storyId}`)} type="button">
      <span className={styles.title}>✨ {status.title || 'Казка'}</span>
      <span className={styles.bar}>
        {phaseText} {status.status !== 'READY' && status.status !== 'FAILED' ? BARS[barIdx] : ''}
      </span>
    </button>
  )
}
