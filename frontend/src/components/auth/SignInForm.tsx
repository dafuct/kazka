import { useState } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useLocale } from '../../lib/LocaleContext'
import { ApiError } from '../../lib/types'
import { useAuthModal } from '../../lib/AuthModalContext'
import styles from './AuthModal.module.css'

export function SignInForm({ onSuccess }: { onSuccess: () => void }) {
  const { signIn } = useAuth()
  const { setTab } = useAuthModal()
  const { t } = useLocale()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handle(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await signIn(email.trim(), password)
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
        <span className={styles.label}>{t.auth.fields.password}</span>
        <input className={styles.input} type="password" required value={password}
               onChange={e => setPassword(e.target.value)} disabled={submitting} />
      </label>
      <button type="button" className={styles.linkBtn} onClick={() => setTab('forgot')}>
        {t.auth.tabs.forgot}
      </button>
      <button type="submit" className={styles.submit} disabled={submitting}>
        {submitting ? '…' : t.auth.actions.signIn}
      </button>
    </form>
  )
}
