import type { CharacterDto } from '@kazka/shared'
import { useLocale } from '../../lib/LocaleContext'
import styles from './CharacterCard.module.css'

export function CharacterCard({ c, onArchive }: { c: CharacterDto; onArchive: () => void }) {
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const kinds = tc.kinds ?? { boy: 'boy', girl: 'girl', animal: 'animal', creature: 'creature', object: 'object' }
  const kindLabel = kinds[c.kind as 'boy' | 'girl' | 'animal' | 'creature' | 'object'] ?? c.kind
  return (
    <article className={styles.card}>
      <header className={styles.head}>
        <h4>{c.name}</h4>
        <span className={styles.kind}>{kindLabel}</span>
      </header>
      <p className={styles.desc}>{c.description}</p>
      <div className={styles.traits}>
        {(c.traits ?? []).map(x => <span key={x} className={styles.trait}>{x}</span>)}
      </div>
      <footer className={styles.foot}>
        <span className={styles.meta}>
          {tc.usageCount ? tc.usageCount(c.usageCount) : `used ${c.usageCount}× `}
        </span>
        <button onClick={onArchive} className={styles.danger}>{tc.archiveCta ?? 'Archive'}</button>
      </footer>
    </article>
  )
}
