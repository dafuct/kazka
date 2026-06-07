import { useCallback, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { useAuthModal } from '../../lib/AuthModalContext'
import { useLocale } from '../../lib/LocaleContext'
import { SignInForm } from './SignInForm'
import { SignUpForm } from './SignUpForm'
import { ForgotPasswordForm } from './ForgotPasswordForm'
import { GoogleButton } from './GoogleButton'
import { AppleButton } from './AppleButton'
import storyStyles from '../modal/StoryModal.module.css'
import styles from './AuthModal.module.css'

export function AuthModal() {
  const { open, tab, closeAuth, setTab } = useAuthModal()
  const { t } = useLocale()
  const navigate = useNavigate()

  const handleSuccess = useCallback(() => {
    closeAuth()
    navigate('/')
  }, [closeAuth, navigate])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') closeAuth() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, closeAuth])

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  if (!open) return null

  return createPortal(
    <div className={storyStyles.backdrop} onClick={closeAuth} role="dialog" aria-modal="true">
      <div className={storyStyles.panel} onClick={e => e.stopPropagation()}>
        <div className={storyStyles.topBorder} />
        <div className={storyStyles.header}>
          <button className={storyStyles.closeBtn} onClick={closeAuth} aria-label="Close">✕</button>
        </div>
        <div className={styles.tabs}>
          <button className={`${styles.tab} ${tab === 'signIn' ? styles.tabActive : ''}`} onClick={() => setTab('signIn')}>
            {t.auth.tabs.signIn}
          </button>
          <button className={`${styles.tab} ${tab === 'signUp' ? styles.tabActive : ''}`} onClick={() => setTab('signUp')}>
            {t.auth.tabs.signUp}
          </button>
        </div>
        <div className={storyStyles.body}>
          {tab === 'signIn' && <SignInForm onSuccess={handleSuccess} />}
          {tab === 'signUp' && <SignUpForm onSuccess={handleSuccess} />}
          {tab === 'forgot' && <ForgotPasswordForm />}
        </div>
        {tab !== 'forgot' && (
          <>
            <div className={styles.divider}>or</div>
            <GoogleButton />
            <AppleButton />
          </>
        )}
      </div>
    </div>,
    document.body,
  )
}
