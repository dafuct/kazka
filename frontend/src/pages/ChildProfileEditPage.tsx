import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ChildProfileForm } from '../components/children/ChildProfileForm'
import { ChildProfileBatchForm } from '../components/children/ChildProfileBatchForm'
import { children as childrenApi } from '../lib/apiClient'
import { useChildren } from '../lib/ChildrenContext'
import { useLocale } from '../lib/LocaleContext'
import { BedtimeSettingsForm } from '../components/children/BedtimeSettingsForm'
import type { ChildProfileDto } from '@kazka/shared'
import styles from './ChildProfileEditPage.module.css'

export function ChildProfileEditPage() {
  const { id } = useParams<{ id: string }>()
  const isNew = !id || id === 'new'
  const navigate = useNavigate()
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const { refetch, setActive } = useChildren()
  const [initial, setInitial] = useState<ChildProfileDto | null>(null)
  const [loading, setLoading] = useState(!isNew)

  useEffect(() => {
    if (isNew) return
    setLoading(true)
    childrenApi.get(id!).then(setInitial).finally(() => setLoading(false))
  }, [id, isNew])

  if (loading) return <p className={styles.loading}>{(t as any).common?.loading ?? 'Loading…'}</p>

  return (
    <div className={`${styles.page} kz-page`}>
      <h1>{isNew ? (tc.newTitle ?? 'Add a child') : (tc.editTitle ?? 'Edit child')}</h1>
      {isNew ? (
        <ChildProfileBatchForm
          submitLabel={tc.createCta ?? 'Create'}
          onSubmit={async (kids) => {
            const created = await childrenApi.createBatch({ children: kids })
            await refetch()
            if (created.length > 0) setActive(created[0].id)
            navigate('/settings/children')
          }}
        />
      ) : (
        <ChildProfileForm
          initial={initial ?? undefined}
          submitLabel={tc.saveCta ?? 'Save'}
          onSubmit={async (v) => {
            const body = {
              name: v.name,
              birthYear: v.birthYear === '' ? undefined : (v.birthYear as number),
              gender: v.gender || undefined,
              preferredLanguage: v.preferredLanguage,
              interests: v.interests,
            }
            await childrenApi.update(id!, body as any)
            await refetch()
            navigate('/settings/children')
          }}
        />
      )}
      {!isNew && (
        <section className={styles.bedtimeSection}>
          <hr className={styles.divider} />
          <h2>{(t as any).children?.bedtime?.sectionTitle ?? 'Bedtime ritual'}</h2>
          <BedtimeSettingsForm childId={id!} />
        </section>
      )}
    </div>
  )
}
