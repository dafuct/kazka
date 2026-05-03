import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import { useTheme } from '../../lib/ThemeContext'
import { useStoryModal } from '../../lib/StoryModalContext'
import styles from './Nav.module.css'

export function Nav() {
  const { toggleLang, t } = useLocale()
  const { theme, toggleTheme } = useTheme()
  const { openModal } = useStoryModal()
  const { pathname } = useLocation()
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const handler = () => setScrolled(window.scrollY > 80)
    window.addEventListener('scroll', handler, { passive: true })
    handler()
    return () => window.removeEventListener('scroll', handler)
  }, [])

  return (
    <nav className={`${styles.nav} ${scrolled ? styles.scrolled : ''}`}>
      <Link to="/" className={styles.logo}>
        <svg viewBox="0 0 28 28" fill="none" className={styles.logoIcon} aria-hidden="true">
          <path d="M6 4C6 4 8 6 8 14C8 22 6 24 6 24" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
          <path d="M6 4C10 4 20 4 22 6C24 8 24 10 22 12C20 14 14 14 14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M6 14C10 14 18 14 20 16C22 18 22 20 20 22C18 24 10 24 6 24" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <circle cx="21" cy="5" r="1.5" fill="#C4B5FD" opacity="0.7"/>
          <circle cx="24" cy="9" r="1" fill="#C4B5FD" opacity="0.5"/>
        </svg>
        <span>Казкар</span>
      </Link>

      <ul className={styles.links}>
        <li>
          <a href="/#how" className={styles.link}>{t.nav.howItWorks}</a>
        </li>
        <li>
          <a href="/#features" className={styles.link}>{t.nav.features}</a>
        </li>
        <li>
          <Link
            to="/stories"
            className={pathname.startsWith('/stories') ? `${styles.link} ${styles.active}` : styles.link}
          >
            {t.nav.archive}
          </Link>
        </li>
        <li>
          <button onClick={toggleTheme} className={styles.themeToggle} aria-label={t.nav.toggleTheme}>
            {theme === 'light' ? t.nav.themeLight : t.nav.themeDark}
          </button>
        </li>
        <li>
          <button onClick={toggleLang} className={styles.langBtn} aria-label="Toggle language">
            {t.nav.toggleLang}
          </button>
        </li>
        <li>
          <a href="#" className={styles.ctaBtn} onClick={(e) => { e.preventDefault(); openModal() }}>{t.nav.tryCta}</a>
        </li>
      </ul>
    </nav>
  )
}
