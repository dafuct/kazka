import { Link } from 'react-router-dom'
import { AvatarInitials } from './AvatarInitials'
import type { ChildProfileDto } from '@kazka/shared'
import { useLocale } from '../../lib/LocaleContext'
import styles from './ChildProfileCard.module.css'

export function ChildProfileCard({ profile, onArchive }: { profile: ChildProfileDto; onArchive: () => void }) {
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const age = profile.birthYear ? new Date().getFullYear() - profile.birthYear : null
  const langs = tc.langs ?? { uk: 'Ukrainian', en: 'English', bilingual: 'Bilingual' }
  const lang = langs[profile.preferredLanguage as 'uk' | 'en' | 'bilingual'] ?? profile.preferredLanguage
  const characterCount = (profile as any).characterCount ?? 0

  return (
    <article className={styles.card}>
      <AvatarInitials name={profile.name} seed={profile.avatarSeed} size={48} />
      <div className={styles.body}>
        <h3>{profile.name}</h3>
        <p className={styles.meta}>
          {age !== null
              ? (tc.ageYears ? tc.ageYears(age) : `${age} yrs`)
              : (tc.ageUnspecified ?? 'age not set')}
          {' · '}{lang}
          {' · '}{tc.characterCount ? tc.characterCount(characterCount) : `${characterCount} characters`}
        </p>
      </div>
      <div className={styles.actions}>
        <Link to={`/settings/children/${profile.id}`}>{tc.editCta ?? 'Edit'}</Link>
        <Link to={`/settings/children/${profile.id}/characters`}>{tc.charactersLink ?? 'Characters'}</Link>
        <button onClick={onArchive} className={styles.danger}>{tc.archiveCta ?? 'Archive'}</button>
      </div>
    </article>
  )
}
