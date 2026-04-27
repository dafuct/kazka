import { Link } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import styles from './NightCta.module.css'

export function NightCta() {
  const { t } = useLocale()

  return (
    <section className={styles.section}>
      <h2 className={styles.title}>{t.nightCta.title}</h2>
      <p className={styles.text}>{t.nightCta.text}</p>
      <Link to="/" className={styles.btn}>{t.nightCta.button}</Link>
    </section>
  )
}
