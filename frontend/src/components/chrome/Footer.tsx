import { Link } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import { DONATE_URL } from '../../lib/config'
import { ScMotif, SCM, THREAD } from '../stitch/StitchCanvas'
import styles from './Footer.module.css'

export function Footer() {
  const { t } = useLocale()
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
      <div className={styles.left}>
        <span className={styles.logoIcon} aria-hidden="true">
          <ScMotif rule={SCM.star8} n={9} stitch={3.6} palette={THREAD} ground={null} />
        </span>
        <span className={styles.tagline}>{t.footer.tagline}</span>
      </div>
      <div className={styles.links}>
        <Link to="/legal/terms" className={styles.link}>{t.footer.terms}</Link>
        <Link to="/legal/privacy" className={styles.link}>{t.footer.privacy}</Link>
        <Link to="/legal/support" className={styles.link}>{t.footer.support}</Link>
        <a href={DONATE_URL} target="_blank" rel="noopener noreferrer" className={styles.link}>{t.footer.donate}</a>
      </div>
      <div className={styles.bottom}>{t.footer.copyright}</div>
      </div>
    </footer>
  )
}
