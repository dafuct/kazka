import { useLocale } from '../../lib/LocaleContext'
import type { ModerationErrorCode, ModerationCategory } from '../../lib/types'
import styles from './RefusalCard.module.css'

interface RefusalCardProps {
  code: ModerationErrorCode
  category?: ModerationCategory
  onTryAnother: () => void
}

export function RefusalCard({ code, category, onTryAnother }: RefusalCardProps) {
  const { t } = useLocale()
  // Prefer the per-category message when the backend told us which rule fired;
  // fall back to the generic BLOCKED_INPUT / JUDGE_UNAVAILABLE message otherwise.
  const message =
    (code === 'BLOCKED_INPUT' && category && t.moderation.byCategory?.[category])
      ?? t.moderation[code]
  return (
    <div className={styles.card} role="alert">
      <p className={styles.message}>{message}</p>
      <button type="button" className={styles.button} onClick={onTryAnother}>
        {t.moderation.tryAnotherTheme}
      </button>
    </div>
  )
}
