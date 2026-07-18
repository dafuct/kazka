import { Link, useNavigate } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import { useAuth } from '../../lib/AuthContext'
import { useAuthModal } from '../../lib/AuthModalContext'
import { DONATE_URL } from '../../lib/config'
import styles from './Footer.module.css'

export function Footer() {
  const { t } = useLocale()
  const { user } = useAuth()
  const { openAuth } = useAuthModal()
  const navigate = useNavigate()

  const goCreate = () => {
    if (!user) openAuth('signUp')
    else navigate('/create')
  }

  return (
    <footer className={styles.footer}>
      <div className="wrap">
        <div className={styles.grid}>
          <div className={styles.col}>
            <div className={styles.logo}>
              <span className={styles.mark} aria-hidden="true">✦</span>
              <span>{t.brand}</span>
            </div>
            <p className={styles.tagline}>{t.footer.tagline}</p>
          </div>
          <div className={styles.col}>
            <h4>{t.footer.colCreate}</h4>
            <button type="button" className={styles.flink} onClick={goCreate}>{t.footer.linkNew}</button>
          </div>
          <div className={styles.col}>
            <h4>{t.footer.colStories}</h4>
            <Link className={styles.flink} to="/showcase">{t.footer.linkAll}</Link>
            {user && <Link className={styles.flink} to="/stories">{t.footer.linkMine}</Link>}
          </div>
          <div className={styles.col}>
            <h4>{t.footer.colAbout}</h4>
            <Link className={styles.flink} to="/legal/support">{t.footer.linkSupport}</Link>
            <a className={styles.flink} href={DONATE_URL} target="_blank" rel="noopener noreferrer">{t.footer.donate}</a>
          </div>
        </div>
        <div className={styles.bottom}>
          <span>{t.footer.copyright}</span>
          <span className={styles.legal}>
            <Link className={styles.flink} to="/legal/privacy">{t.footer.privacy}</Link>
            <Link className={styles.flink} to="/legal/terms">{t.footer.terms}</Link>
          </span>
        </div>
      </div>
    </footer>
  )
}
