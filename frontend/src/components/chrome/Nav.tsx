import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import { useTheme } from '../../lib/ThemeContext'
import { useStoryModal } from '../../lib/StoryModalContext'
import { useAuth } from '../../lib/AuthContext'
import { useAuthModal } from '../../lib/AuthModalContext'
import { useBilling } from '../../lib/BillingContext'
import { ActiveChildPicker } from '../children/ActiveChildPicker'
import styles from './Nav.module.css'

export function Nav() {
  const { toggleLang, t } = useLocale()
  const { theme, toggleTheme } = useTheme()
  const { openModal } = useStoryModal()
  const { user, signOut } = useAuth()
  const { openAuth } = useAuthModal()
  const { isPro } = useBilling()
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
        <svg viewBox="0 0 28 28" fill="none" className={styles.logoIcon} aria-hidden="true">
          <defs>
            <mask id="kazkaMoonMask">
              <rect width="28" height="28" fill="white"/>
              <circle cx="15" cy="11" r="6" fill="black"/>
            </mask>
          </defs>
          <circle cx="11" cy="14" r="7" fill="var(--color-magic)" mask="url(#kazkaMoonMask)"/>
          <path d="M22 2 L22.9 4.1 L25 5 L22.9 5.9 L22 8 L21.1 5.9 L19 5 L21.1 4.1 Z" fill="var(--color-gold)"/>
        </svg>
        <span>{t.brand}</span>
      </Link>

      <ul className={styles.links}>
        {!user && (
          <>
            <li><a href="/#how" className={styles.link}>{t.nav.howItWorks}</a></li>
            <li><a href="/#features" className={styles.link}>{t.nav.features}</a></li>
          </>
        )}
        {!isPro && (
          <li>
            <Link to="/pricing"
                  className={pathname.startsWith('/pricing') ? `${styles.link} ${styles.active}` : styles.link}>
              {t.nav.pricing}
            </Link>
          </li>
        )}
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
                {user.role === 'ADMIN'
                  ? <span className={styles.proBadge}>Admin</span>
                  : isPro && <span className={styles.proBadge}>Pro</span>}
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
