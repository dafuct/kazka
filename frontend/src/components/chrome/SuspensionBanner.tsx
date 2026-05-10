import { useAuth } from '../../lib/AuthContext'
import { useLocale } from '../../lib/LocaleContext'
import styles from './SuspensionBanner.module.css'

const SUPPORT_EMAIL = 'support@kazka.local'

export function SuspensionBanner() {
  const { user } = useAuth()
  const { t } = useLocale()
  if (!user?.suspended) return null
  return (
    <div className={styles.banner} role="alert">
      <span>{t.moderation.accountSuspended}</span>
      <a className={styles.link} href={`mailto:${SUPPORT_EMAIL}`}>
        {SUPPORT_EMAIL}
      </a>
    </div>
  )
}
