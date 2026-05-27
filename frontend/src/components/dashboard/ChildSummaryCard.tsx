import { Link } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import type { ChildSummary } from '@kazka/shared'
import styles from './ChildSummaryCard.module.css'

export interface ChildSummaryCardProps {
  child: ChildSummary
}

export function ChildSummaryCard({ child }: ChildSummaryCardProps) {
  const { t } = useLocale()
  const td = (t as any).dashboard ?? {}

  return (
    <div className={styles.card}>
      <div className={styles.head}>
        <h3 className={styles.name}>{child.name}</h3>
        <span className={styles.count}>{child.taleCount} {td.tales ?? 'tales'}</span>
      </div>
      {child.latestTale ? (
        <Link to={`/stories/${child.latestTale.id}`} className={styles.latest}>
          <span className={styles.latestLabel}>{td.latest ?? 'Latest'}:</span> {child.latestTale.title || (td.untitled ?? 'Untitled')}
        </Link>
      ) : (
        <p className={styles.empty}>{td.noTales ?? 'No tales yet'}</p>
      )}
      {child.lastBedtimeAt && (
        <p className={styles.bedtime}>
          {td.lastBedtime ?? 'Last bedtime'}: {formatRelative(child.lastBedtimeAt)}
        </p>
      )}
    </div>
  )
}

function formatRelative(isoString: string): string {
  const date = new Date(isoString)
  const days = Math.floor((Date.now() - date.getTime()) / 86400000)
  if (days === 0) return 'today'
  if (days === 1) return 'yesterday'
  return `${days} days ago`
}
