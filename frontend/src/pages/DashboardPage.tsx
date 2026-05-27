import { useEffect, useState } from 'react'
import { dashboard as dashboardApi } from '../lib/apiClient'
import { useLocale } from '../lib/LocaleContext'
import type { Dashboard } from '@kazka/shared'
import styles from './DashboardPage.module.css'

export function DashboardPage() {
  const { t } = useLocale()
  const td = (t as any).dashboard ?? {}
  const [data, setData] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    dashboardApi.get()
      .then(setData)
      .catch((e: any) => setError(e?.message ?? (td.error ?? 'Could not load dashboard')))
  }, [])

  if (error) return <div className={styles.page}><p className={styles.error}>{error}</p></div>
  if (!data) return <div className={styles.page}><p className={styles.loading}>{td.loading ?? 'Loading…'}</p></div>

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>{td.title ?? 'Dashboard'}</h1>
        <span className={data.isPro ? styles.pillPro : styles.pillFree}>
          {data.isPro ? (td.pillPro ?? '⭐ Pro') : (td.pillFree ?? 'Free')}
        </span>
      </header>
      <pre className={styles.debug}>{JSON.stringify(data, null, 2)}</pre>
    </div>
  )
}
