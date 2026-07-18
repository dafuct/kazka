import { Link } from 'react-router-dom'
import { useChildren } from '../lib/ChildrenContext'
import { children as childrenApi } from '../lib/apiClient'
import { useLocale } from '../lib/LocaleContext'
import { ChildProfileCard } from '../components/children/ChildProfileCard'
import styles from './ChildrenListPage.module.css'

export function ChildrenListPage() {
  const { children: profiles, refetch, loading } = useChildren()
  const { t } = useLocale()
  const tc = (t as any).children ?? {}

  async function archive(id: string) {
    if (!confirm(tc.archiveConfirm ?? 'Archive this profile?')) return
    await childrenApi.archive(id)
    await refetch()
  }

  return (
    <div className={`${styles.page} kz-page`}>
      <header className={styles.header}>
        <h1>{tc.listTitle ?? 'Children'}</h1>
        <Link to="/settings/children/new" className={styles.addBtn}>+ {tc.addChild ?? 'Add child'}</Link>
      </header>
      {loading && <p>{(t as any).common?.loading ?? 'Loading…'}</p>}
      {!loading && profiles.length === 0 && (
        <p className={styles.empty}>{tc.emptyState ?? 'Add your first child to start creating tales.'}</p>
      )}
      <ul className={styles.list}>
        {profiles.map(p => (
          <li key={p.id}>
            <ChildProfileCard profile={p} onArchive={() => archive(p.id)} />
          </li>
        ))}
      </ul>
    </div>
  )
}
