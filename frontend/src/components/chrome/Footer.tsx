import { useLocale } from '../../lib/LocaleContext'
import styles from './Footer.module.css'

export function Footer() {
  const { t } = useLocale()
  return (
    <footer className={styles.footer}>
      <div className={styles.left}>
        <svg viewBox="0 0 28 28" fill="none" className={styles.logoIcon} aria-hidden="true">
          <path d="M6 4C6 4 8 6 8 14C8 22 6 24 6 24" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
          <path d="M6 4C10 4 20 4 22 6C24 8 24 10 22 12C20 14 14 14 14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M6 14C10 14 18 14 20 16C22 18 22 20 20 22C18 24 10 24 6 24" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
        <span className={styles.tagline}>{t.footer.tagline}</span>
      </div>
      <div className={styles.links}>
        <a href="#" className={styles.link}>{t.footer.terms}</a>
        <a href="#" className={styles.link}>{t.footer.privacy}</a>
        <a href="#" className={styles.link}>{t.footer.support}</a>
      </div>
      <div className={styles.bottom}>{t.footer.copyright}</div>
    </footer>
  )
}
