import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import { useAuth } from '../../lib/AuthContext'
import { useAuthModal } from '../../lib/AuthModalContext'
import { DONATE_URL } from '../../lib/config'
import { ActiveChildPicker } from '../children/ActiveChildPicker'
import styles from './Nav.module.css'

export function Nav() {
  const { t, lang, toggleLang } = useLocale()
  const { user, signOut } = useAuth()
  const { openAuth } = useAuthModal()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('click', onClick)
    return () => document.removeEventListener('click', onClick)
  }, [])

  const linkCls = ({ isActive }: { isActive: boolean }) =>
    isActive ? `${styles.link} ${styles.on}` : styles.link

  const goCreate = () => {
    setMobileOpen(false)
    if (!user) openAuth('signUp')
    else navigate('/create')
  }

  const links = (
    <>
      <NavLink to="/" end className={linkCls} onClick={() => setMobileOpen(false)}>{t.nav.home}</NavLink>
      <NavLink to="/showcase" className={linkCls} onClick={() => setMobileOpen(false)}>{t.nav.stories}</NavLink>
      <a
        href="/create"
        className={styles.link}
        onClick={e => { e.preventDefault(); goCreate() }}
      >{t.nav.create}</a>
      {user && (
        <NavLink to="/stories" className={linkCls} onClick={() => setMobileOpen(false)}>{t.nav.myTales}</NavLink>
      )}
      {user && (
        <NavLink to="/dashboard" className={linkCls} onClick={() => setMobileOpen(false)}>{t.nav.dashboard}</NavLink>
      )}
    </>
  )

  return (
    <>
      <nav className={styles.nav}>
        <div className={`wrap ${styles.inner}`}>
          <Link to="/" className={styles.logo}>
            <span className={styles.mark} aria-hidden="true">✦</span>
            <span>{t.brand}</span>
          </Link>

          <div className={styles.links}>{links}</div>

          <div className={styles.right}>
            {user && <ActiveChildPicker />}
            <div className={styles.langPill}>
              {(['uk', 'en'] as const).map(lg => (
                <button
                  key={lg}
                  type="button"
                  className={lang === lg ? `${styles.langOpt} ${styles.langOn}` : styles.langOpt}
                  onClick={() => { if (lang !== lg) toggleLang() }}
                >
                  {lg === 'uk' ? 'UA' : 'EN'}
                </button>
              ))}
            </div>
            <a
              href={DONATE_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={`icon-btn ${styles.donate}`}
              title={t.nav.donate}
            >♥</a>
            {!user && (
              <button type="button" className={styles.signIn} onClick={() => openAuth('signIn')}>
                {t.auth.tabs.signIn}
              </button>
            )}
            {(!user || !user.suspended) && (
              <button type="button" className={`btn btn-primary ${styles.cta}`} onClick={goCreate}>
                {t.nav.cta}
              </button>
            )}
            {user && (
              <div className={styles.userWrap} ref={menuRef}>
                <button type="button" className={styles.userBtn} onClick={() => setMenuOpen(o => !o)}>
                  <span>{user.displayName}</span>
                  {user.role === 'ADMIN' && <span className={styles.adminBadge}>Admin</span>}
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
              </div>
            )}
            <button
              type="button"
              className={`icon-btn ${styles.burger}`}
              aria-label="Menu"
              onClick={() => setMobileOpen(o => !o)}
            >
              {mobileOpen ? '✕' : '☰'}
            </button>
          </div>
        </div>
      </nav>
      {mobileOpen && (
        <div className={styles.mmenu}>
          {links}
          <button type="button" className="btn btn-primary btn-lg" onClick={goCreate}>
            {t.nav.cta}
          </button>
        </div>
      )}
    </>
  )
}
