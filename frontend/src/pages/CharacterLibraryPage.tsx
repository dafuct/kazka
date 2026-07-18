import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { children as childrenApi, charactersApi } from '../lib/apiClient'
import { useLocale } from '../lib/LocaleContext'
import type { CharacterDto } from '@kazka/shared'
import { CharacterCard } from '../components/children/CharacterCard'
import styles from './CharacterLibraryPage.module.css'

export function CharacterLibraryPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const [list, setList] = useState<CharacterDto[]>([])
  const [loading, setLoading] = useState(true)

  async function load() {
    if (!id) return
    setLoading(true)
    try { setList(await childrenApi.listCharacters(id)) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [id])

  async function archive(charId: string) {
    if (!confirm(tc.archiveConfirm ?? 'Archive this character?')) return
    await charactersApi.archive(charId)
    await load()
  }

  return (
    <div className={`${styles.page} kz-page`}>
      <header className={styles.header}>
        <h1>{tc.libraryTitle ?? 'Character library'}</h1>
        <Link to={`/settings/children/${id}`}>← {tc.backToProfile ?? 'Back to profile'}</Link>
      </header>
      {loading && <p>{(t as any).common?.loading ?? 'Loading…'}</p>}
      {!loading && list.length === 0 && (
        <p className={styles.empty}>{tc.libraryEmpty ?? 'No saved characters yet.'}</p>
      )}
      <ul className={styles.list}>
        {list.map(c => (
          <li key={c.id}><CharacterCard c={c} onArchive={() => archive(c.id)} /></li>
        ))}
      </ul>
    </div>
  )
}
