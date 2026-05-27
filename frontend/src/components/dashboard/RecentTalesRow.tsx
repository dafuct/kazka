import { Link } from 'react-router-dom'
import type { Story } from '@kazka/shared'
import styles from './RecentTalesRow.module.css'

export interface RecentTalesRowProps {
  tales: Story[]
}

export function RecentTalesRow({ tales }: RecentTalesRowProps) {
  if (tales.length === 0) return null
  return (
    <div className={styles.row}>
      {tales.map(t => (
        <Link key={t.id} to={`/stories/${t.id}`} className={styles.tile}>
          {t.illustrationPathDark || t.illustrationPathLight ? (
            <img src={t.illustrationPathDark ?? t.illustrationPathLight ?? ''} alt="" className={styles.thumb} />
          ) : (
            <div className={styles.placeholder} />
          )}
          <span className={styles.title}>{t.title || t.theme}</span>
        </Link>
      ))}
    </div>
  )
}
