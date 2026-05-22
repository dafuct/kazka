import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import styles from './SubscriptionSuccessPage.module.css'
import { useLocale } from '../lib/LocaleContext'
import { useBilling } from '../lib/BillingContext'

const POLL_INTERVAL_MS = 1000
const POLL_LIMIT = 10

export function SubscriptionSuccessPage() {
  const { t } = useLocale()
  const { isPro, refresh } = useBilling()
  const [params] = useSearchParams()
  const [attempts, setAttempts] = useState(0)
  const redirect = params.get('redirect') ?? '/stories'

  useEffect(() => {
    if (isPro) {
      window.location.replace(redirect)
      return
    }
    if (attempts >= POLL_LIMIT) return
    const id = window.setTimeout(() => {
      refresh().finally(() => setAttempts(a => a + 1))
    }, POLL_INTERVAL_MS)
    return () => window.clearTimeout(id)
  }, [attempts, isPro, refresh, redirect])

  const giveUp = attempts >= POLL_LIMIT && !isPro

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>{t.pricing.success.title}</h1>
      {!giveUp && <p className={styles.msg}>{t.pricing.success.activating}</p>}
      {giveUp && (
        <>
          <p className={styles.msg}>{t.pricing.success.delayed}</p>
          <Link to="/" className={styles.cta}>{t.pricing.success.continue}</Link>
        </>
      )}
    </div>
  )
}
