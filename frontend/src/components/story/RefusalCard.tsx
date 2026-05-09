import { useLocale } from '../../lib/LocaleContext'
import type { ModerationErrorCode } from '../../lib/types'
import styles from './RefusalCard.module.css'

interface RefusalCardProps {
  code: ModerationErrorCode
  onTryAnother: () => void
}

export function RefusalCard({ code, onTryAnother }: RefusalCardProps) {
  const { t } = useLocale()
  const message = t.moderation[code]
  return (
    <div className={styles.card} role="alert">
      <p className={styles.message}>{message}</p>
      <button type="button" className={styles.button} onClick={onTryAnother}>
        {t.moderation.tryAnotherTheme}
      </button>
    </div>
  )
}
