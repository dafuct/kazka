import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { useLocale } from '../lib/LocaleContext'
import { ApiError } from '../lib/types'
import styles from './PasswordResetPage.module.css'

export function PasswordResetPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const { confirmPasswordReset } = useAuth()
  const { openAuth } = useAuthModal()
  const { t } = useLocale()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  if (!token) {
    return <div className={styles.page}><h1>{t.auth.errors.TOKEN_INVALID}</h1></div>
  }

  if (done) {
    return (
      <div className={styles.page}>
        <h1>{t.auth.messages.passwordUpdated}</h1>
        <button className={styles.btn} onClick={() => openAuth('signIn')}>
          {t.auth.tabs.signIn}
        </button>
      </div>
    )
  }

  async function handle(e: React.FormEvent) {
    e.preventDefault()
    if (password !== confirm) { setError(t.auth.errors.passwordMismatch); return }
    if (password.length < 8) { setError(t.auth.errors.passwordTooShort); return }
    setSubmitting(true)
    try {
      await confirmPasswordReset(token, password)
      setDone(true)
    } catch (err) {
      const code = err instanceof ApiError ? err.body.error : 'ERROR'
      setError(t.auth.errors[code as keyof typeof t.auth.errors] ?? t.auth.errors.ERROR)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.heading}>{t.auth.actions.submitReset}</h1>
      <form onSubmit={handle} className={styles.form}>
        {error && <div className={styles.banner}>{error}</div>}
        <input className={styles.input} type="password" required minLength={8}
               placeholder={t.auth.fields.newPassword}
               value={password} onChange={e => setPassword(e.target.value)} disabled={submitting} />
        <input className={styles.input} type="password" required minLength={8}
               placeholder={t.auth.fields.confirmPassword}
               value={confirm} onChange={e => setConfirm(e.target.value)} disabled={submitting} />
        <button className={styles.btn} type="submit" disabled={submitting}>
          {submitting ? '…' : t.auth.actions.submitReset}
        </button>
      </form>
    </div>
  )
}
