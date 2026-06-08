import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import { useTheme } from '../../lib/ThemeContext'
import { useStoryModal } from '../../lib/StoryModalContext'
import { useAuth } from '../../lib/AuthContext'
import { useAuthModal } from '../../lib/AuthModalContext'
import { DONATE_URL } from '../../lib/config'
import { ActiveChildPicker } from '../children/ActiveChildPicker'
import { ScMotif, SCM, THREAD } from '../stitch/StitchCanvas'
import styles from './Nav.module.css'

export function Nav() {
  const { toggleLang, t } = useLocale()
  const { theme, toggleTheme } = useTheme()
  const { openModal } = useStoryModal()
  const { user, signOut } = useAuth()
  const { openAuth } = useAuthModal()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [scrolled, setScrolled] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLLIElement>(null)

  useEffect(() => {
    const handler = () => setScrolled(window.scrollY > 80)
    window.addEventListener('scroll', handler, { passive: true })
    handler()
    return () => window.removeEventListener('scroll', handler)
  }, [])

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('click', onClick)
    return () => document.removeEventListener('click', onClick)
  }, [])

  const tryClick = (e: React.MouseEvent) => {
    e.preventDefault()
    if (!user) openAuth('signIn'); else openModal()
  }

  return (
    <nav className={`${styles.nav} ${scrolled ? styles.scrolled : ''}`}>
      <Link to="/" className={styles.logo}>
        <span className={styles.logoIcon} aria-hidden="true">
          <ScMotif rule={SCM.star8} n={11} stitch={4.5} palette={THREAD} ground={null} />
        </span>
        <span>{t.brand}</span>
      </Link>

      <ul className={styles.links}>
        <li><a href="/#how" className={styles.link}>{t.nav.howItWorks}</a></li>
        <li><a href="/#features" className={styles.link}>{t.nav.features}</a></li>
        <li>
          <Link to="/showcase"
                className={pathname.startsWith('/showcase') ? `${styles.link} ${styles.active}` : styles.link}>
            {t.nav.sampleTales}
          </Link>
        </li>
        <li>
          <a href={DONATE_URL} target="_blank" rel="noopener noreferrer" className={styles.donateBtn}>
            {t.nav.donate}
          </a>
        </li>
        {user && (
          <li>
            <Link to="/stories"
                  className={pathname.startsWith('/stories') ? `${styles.link} ${styles.active}` : styles.link}>
              {t.nav.archive}
            </Link>
          </li>
        )}
        {user && (
          <li>
            <Link to="/dashboard"
                  className={pathname.startsWith('/dashboard') ? `${styles.link} ${styles.active}` : styles.link}>
              {t.nav.dashboard}
            </Link>
          </li>
        )}
        {user && (
          <li className={styles.childPickerItem}>
            <ActiveChildPicker />
          </li>
        )}
        <li>
          <button onClick={toggleTheme} className={styles.themeToggle} aria-label={t.nav.toggleTheme}>
            <span className={styles.themeIcon} aria-hidden="true">{theme === 'light' ? '🌙' : '☀️'}</span>
            <span className={styles.themeLabel}>{theme === 'light' ? t.nav.themeLight : t.nav.themeDark}</span>
          </button>
        </li>
        <li>
          <button onClick={toggleLang} className={styles.langBtn} aria-label="Toggle language">
            {t.nav.toggleLang}
          </button>
        </li>
        {!user && (
          <>
            <li>
              <button className={styles.link} onClick={() => openAuth('signIn')}>{t.auth.tabs.signIn}</button>
            </li>
            <li>
              <button className={styles.ctaBtn} onClick={() => openAuth('signUp')}>{t.auth.tabs.signUp}</button>
            </li>
          </>
        )}
        {user && (
          <>
            {!user.suspended && (
              <li>
                <a href="#" className={styles.ctaBtn} onClick={tryClick}>{t.nav.tryCta}</a>
              </li>
            )}
            <li className={styles.userWrap} ref={menuRef}>
              <button className={styles.userBtn} onClick={() => setMenuOpen(o => !o)}>
                <span>{user.displayName}</span>
                {user.role === 'ADMIN' && <span className={styles.proBadge}>Admin</span>}
                <span aria-hidden="true">▾</span>
              </button>
              {menuOpen && (
                <div className={styles.userMenu}>
                  <button onClick={() => { setMenuOpen(false); navigate('/stories') }}>{t.auth.actions.myArchive}</button>
                  <button onClick={() => { setMenuOpen(false); navigate('/settings') }}>{t.auth.actions.settings}</button>
                  {user.role === 'ADMIN' && (
                    <button onClick={() => { setMenuOpen(false); navigate('/admin/users') }}>{t.auth.actions.adminUsers}</button>
                  )}
                  {user.role === 'ADMIN' && (
                    <button onClick={() => { setMenuOpen(false); navigate('/admin/moderation') }}>Admin → Moderation</button>
                  )}
                  <button onClick={async () => { setMenuOpen(false); await signOut(); navigate('/') }}>{t.auth.actions.signOut}</button>
                </div>
              )}
            </li>
          </>
        )}
      </ul>
    </nav>
  )
}
