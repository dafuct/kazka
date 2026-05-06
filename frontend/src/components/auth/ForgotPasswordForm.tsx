import { useState } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useLocale } from '../../lib/LocaleContext'
import styles from './AuthModal.module.css'

export function ForgotPasswordForm() {
  const { requestPasswordReset } = useAuth()
  const { t } = useLocale()
  const [email, setEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  async function handle(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      await requestPasswordReset(email.trim())
      setDone(true)
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return <div className={`${styles.banner} ${styles.bannerInfo}`}>{t.auth.messages.resetSent}</div>
  }

  return (
    <form className={styles.form} onSubmit={handle}>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.email}</span>
        <input className={styles.input} type="email" required value={email}
               onChange={e => setEmail(e.target.value)} disabled={submitting} />
      </label>
      <button type="submit" className={styles.submit} disabled={submitting}>
        {submitting ? '…' : t.auth.actions.sendResetLink}
      </button>
    </form>
  )
}
