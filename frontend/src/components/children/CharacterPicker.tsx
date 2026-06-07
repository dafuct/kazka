import { useEffect, useState } from 'react'
import { useChildren } from '../../lib/ChildrenContext'
import { useLocale } from '../../lib/LocaleContext'
import { children as childrenApi } from '../../lib/apiClient'
import type { CharacterDto } from '../../lib/types'
import styles from './CharacterPicker.module.css'

export function CharacterPicker({
  selected, onChange,
}: { selected: string[]; onChange: (ids: string[]) => void }) {
  const { active } = useChildren()
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const [list, setList] = useState<CharacterDto[]>([])

  useEffect(() => {
    if (!active) { setList([]); return }
    childrenApi.listCharacters(active.id).then(setList).catch(() => setList([]))
  }, [active])

  if (!active) return null

  if (list.length === 0) {
    return <p className={styles.empty}>{tc.noCharactersYet ?? 'No saved characters yet — generate your first tale.'}</p>
  }

  function toggle(id: string) {
    if (selected.includes(id)) onChange(selected.filter(x => x !== id))
    else if (selected.length < 3) onChange([...selected, id])
  }

  return (
    <div>
      <p className={styles.hint}>{tc.pickUpTo3 ?? 'Pick up to 3 characters who will appear in this tale.'}</p>
      <ul className={styles.chips}>
        {list.map(c => (
          <li key={c.id}>
            <button type="button"
                    className={selected.includes(c.id) ? styles.chipOn : styles.chip}
                    onClick={() => toggle(c.id)}>
              {c.name}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
