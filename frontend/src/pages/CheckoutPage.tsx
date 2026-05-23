import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import styles from './CheckoutPage.module.css'
import { openPaddleCheckout } from '../lib/paddle'
import { useTheme } from '../lib/ThemeContext'

export function CheckoutPage() {
  const [params] = useSearchParams()
  const { theme } = useTheme()
  const [error, setError] = useState<string | null>(null)
  const transactionId = params.get('_ptxn')

  useEffect(() => {
    if (!transactionId) {
      setError('Missing transaction ID in URL')
      return
    }
    openPaddleCheckout({
      transactionId,
      successUrl: `${window.location.origin}/subscription/success?redirect=/stories`,
      theme: theme === 'dark' ? 'dark' : 'light',
    }).catch(e => {
      setError(e instanceof Error ? e.message : 'Failed to open checkout')
    })
  }, [transactionId, theme])

  return (
    <div className={styles.page}>
      {error ? (
        <>
          <h1 className={styles.title}>Checkout unavailable</h1>
          <p className={styles.error}>{error}</p>
        </>
      ) : (
        <>
          <h1 className={styles.title}>Opening secure checkout…</h1>
          <p className={styles.msg}>If the checkout window doesn't open, refresh the page.</p>
        </>
      )}
    </div>
  )
}
