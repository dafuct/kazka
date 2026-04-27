import { Link, useLocation } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import { useTheme } from '../../lib/ThemeContext'
import styles from './Nav.module.css'

export function Nav() {
  const { t, toggleLang } = useLocale()
  const { theme, toggleTheme } = useTheme()
  const { pathname } = useLocation()

  return (
    <header className={styles.header}>
      <nav className={styles.nav}>
        <Link to="/" className={styles.brand}>
          {t.home.hero}
        </Link>
        <div className={styles.links}>
          <Link
            to="/"
            className={pathname === '/' ? `${styles.link} ${styles.active}` : styles.link}
          >
            {t.nav.home}
          </Link>
          <Link
            to="/stories"
            className={pathname.startsWith('/stories') ? `${styles.link} ${styles.active}` : styles.link}
          >
            {t.nav.archive}
          </Link>
        </div>
        <div className={styles.actions}>
          <button onClick={toggleTheme} className={styles.iconBtn} aria-label={t.nav.toggleTheme}>
            {theme === 'light' ? '🌙' : '☀️'}
          </button>
          <button onClick={toggleLang} className={styles.langBtn}>
            {t.nav.toggleLang}
          </button>
        </div>
      </nav>
    </header>
  )
}
