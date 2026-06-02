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
      {tales.map(t => {
        const cover = t.panels[0]?.imageUrl
        return (
          <Link key={t.id} to={`/stories/${t.id}`} className={styles.tile}>
            {cover ? (
              <img src={cover} alt="" className={styles.thumb} />
            ) : (
              <div className={`${styles.placeholder} ${styles.skeleton}`} aria-label="Генерується…" />
            )}
            <span className={styles.title}>{t.title || t.theme}</span>
          </Link>
        )
      })}
    </div>
  )
}
