import { useLocale } from '../../lib/LocaleContext'
import styles from './AuthModal.module.css'

export function GoogleButton() {
  const { t } = useLocale()
  return (
    <a className={styles.googleBtn} href="/oauth2/authorization/google">
      {t.auth.actions.google}
    </a>
  )
}
