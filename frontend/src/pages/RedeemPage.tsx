import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { gift } from '../lib/apiClient'
import { useLocale } from '../lib/LocaleContext'
import styles from './RedeemPage.module.css'

export function RedeemPage() {
  const { t } = useLocale()
  const tr = (t as any).redeem ?? {}
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<{ expiresAt: string } | null>(null)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!code.trim()) return
    setBusy(true)
    setError(null)
    try {
      const r = await gift.redeem(code)
      setResult({ expiresAt: r.expiresAt ?? new Date().toISOString() })
      setTimeout(() => navigate('/dashboard'), 1800)
    } catch (err: any) {
      const status = err?.status
      if (status === 404) setError(tr.notFound ?? 'Code not found')
      else if (status === 410) setError(tr.alreadyUsed ?? 'This code is no longer valid')
      else setError(err?.message ?? (tr.genericError ?? 'Could not redeem the code'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>{tr.title ?? 'Redeem gift code'}</h1>
      {result ? (
        <div className={styles.success}>
          <p>{tr.success ?? 'Code redeemed! Your Pro expires:'}</p>
          <p className={styles.date}>{new Date(result.expiresAt).toLocaleDateString()}</p>
          <p className={styles.muted}>{tr.redirecting ?? 'Redirecting to dashboard…'}</p>
        </div>
      ) : (
        <form onSubmit={onSubmit} className={styles.form}>
          <label className={styles.field}>
            <span className={styles.label}>{tr.codeLabel ?? 'Your code'}</span>
            <input
              type="text"
              value={code}
              onChange={e => setCode(e.target.value)}
              placeholder="KAZK-A1B2"
              className={styles.input}
              autoFocus
              disabled={busy}
            />
          </label>
          {error && <p className={styles.error}>{error}</p>}
          <button type="submit" disabled={busy || !code.trim()} className={styles.submit}>
            {busy ? (tr.submitting ?? 'Redeeming…') : (tr.submit ?? 'Redeem')}
          </button>
        </form>
      )}
    </div>
  )
}
