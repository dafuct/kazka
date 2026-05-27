import { useEffect, useState } from 'react'
import { dashboard as dashboardApi } from '../lib/apiClient'
import { useLocale } from '../lib/LocaleContext'
import type { Dashboard } from '@kazka/shared'
import styles from './DashboardPage.module.css'
import { StatCard } from '../components/dashboard/StatCard'
import { ChildSummaryCard } from '../components/dashboard/ChildSummaryCard'
import { RecentTalesRow } from '../components/dashboard/RecentTalesRow'
import { QuickLinks } from '../components/dashboard/QuickLinks'

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
      <section className={styles.statsRow}>
        <StatCard label={td.thisWeek ?? 'This week'} value={data.aggregates.talesThisWeek} />
        <StatCard label={td.thisMonth ?? 'This month'} value={data.aggregates.talesThisMonth} />
        <StatCard label={td.total ?? 'Total'} value={data.aggregates.talesTotal} />
      </section>

      {data.children.length > 0 && (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>{td.childrenSection ?? 'Children'}</h2>
          <div className={styles.childrenGrid}>
            {data.children.map(c => <ChildSummaryCard key={c.childProfileId} child={c} />)}
          </div>
        </section>
      )}

      {data.recentTales.length > 0 && (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>{td.recent ?? 'Recent tales'}</h2>
          <RecentTalesRow tales={data.recentTales} />
        </section>
      )}

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{td.quickLinks ?? 'Quick links'}</h2>
        <QuickLinks />
      </section>
    </div>
  )
}
