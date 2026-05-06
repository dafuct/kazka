import { useState } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useLocale } from '../../lib/LocaleContext'
import { ApiError } from '../../lib/types'
import styles from './AuthModal.module.css'

export function SignUpForm({ onSuccess }: { onSuccess: () => void }) {
  const { signUp } = useAuth()
  const { t } = useLocale()
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handle(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (password !== confirm) { setError(t.auth.errors.passwordMismatch); return }
    if (password.length < 8) { setError(t.auth.errors.passwordTooShort); return }
    setSubmitting(true)
    try {
      await signUp(email.trim(), password, displayName.trim())
      onSuccess()
    } catch (err) {
      const code = err instanceof ApiError ? err.body.error : 'ERROR'
      setError(t.auth.errors[code as keyof typeof t.auth.errors] ?? t.auth.errors.ERROR)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className={styles.form} onSubmit={handle}>
      {error && <div className={styles.banner}>{error}</div>}
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.email}</span>
        <input className={styles.input} type="email" required value={email}
               onChange={e => setEmail(e.target.value)} disabled={submitting} />
      </label>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.displayName}</span>
        <input className={styles.input} type="text" required maxLength={100} value={displayName}
               onChange={e => setDisplayName(e.target.value)} disabled={submitting} />
      </label>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.password}</span>
        <input className={styles.input} type="password" required minLength={8} value={password}
               onChange={e => setPassword(e.target.value)} disabled={submitting} />
      </label>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.confirmPassword}</span>
        <input className={styles.input} type="password" required minLength={8} value={confirm}
               onChange={e => setConfirm(e.target.value)} disabled={submitting} />
      </label>
      <button type="submit" className={styles.submit} disabled={submitting}>
        {submitting ? '…' : t.auth.actions.signUp}
      </button>
    </form>
  )
}
