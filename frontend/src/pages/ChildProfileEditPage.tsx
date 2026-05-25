import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ChildProfileForm } from '../components/children/ChildProfileForm'
import { children as childrenApi } from '../lib/apiClient'
import { useChildren } from '../lib/ChildrenContext'
import { useLocale } from '../lib/LocaleContext'
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
    <div className={styles.page}>
      <h1>{isNew ? (tc.newTitle ?? 'Add a child') : (tc.editTitle ?? 'Edit child')}</h1>
      <ChildProfileForm
        initial={initial ?? undefined}
        submitLabel={isNew ? (tc.createCta ?? 'Create') : (tc.saveCta ?? 'Save')}
        onSubmit={async (v) => {
          const body = {
            name: v.name,
            birthYear: v.birthYear === '' ? undefined : (v.birthYear as number),
            gender: v.gender || undefined,
            preferredLanguage: v.preferredLanguage,
            interests: v.interests,
          }
          if (isNew) {
            const created = await childrenApi.create(body as any)
            await refetch()
            setActive(created.id)
            navigate('/settings/children')
          } else {
            await childrenApi.update(id!, body as any)
            await refetch()
            navigate('/settings/children')
          }
        }}
      />
    </div>
  )
}
