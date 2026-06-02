import type { CSSProperties } from 'react'
import styles from './ComicsPanel.module.css'

interface InProgressCardProps {
  title: string
}

export function InProgressCard({ title }: InProgressCardProps) {
  const style = { '--panel-aspect': '16 / 9' } as CSSProperties
  return (
    <div className={styles.skeleton} aria-label="Генерується…" style={style}>
      <span className={styles.inProgressLabel}>{title || '…'} — генерується</span>
    </div>
  )
}
