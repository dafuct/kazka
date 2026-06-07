import { useState } from 'react'
import { useLocale } from '../../lib/LocaleContext'
import type { ChildProfileDto } from '@kazka/shared'
import styles from './ChildProfileForm.module.css'

export type ChildFormValues = {
  name: string
  birthYear: number | ''
  gender: '' | 'boy' | 'girl' | 'other'
  preferredLanguage: 'uk' | 'en' | 'bilingual'
  interests: string[]
}

export function emptyChildFormValues(): ChildFormValues {
  return { name: '', birthYear: '', gender: '', preferredLanguage: 'uk', interests: [] }
}

/**
 * Controlled field group for a single child. Renders the inputs only — no
 * <form> wrapper and no submit button — so it can be reused both by the
 * single-child {@link ChildProfileForm} (edit flow) and the multi-row batch
 * create form, which own the surrounding <form> and submission.
 */
export interface ChildProfileFieldsProps {
  value: ChildFormValues
  onChange: (v: ChildFormValues) => void
}

export function ChildProfileFields({ value: v, onChange }: ChildProfileFieldsProps) {
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const [interestDraft, setInterestDraft] = useState('')

  function addInterest() {
    const x = interestDraft.trim()
    if (!x) return
    if (v.interests.includes(x)) return
    if (v.interests.length >= 10) return
    onChange({ ...v, interests: [...v.interests, x] })
    setInterestDraft('')
  }

  return (
    <>
      <label className={styles.field}>
        <span>{tc.fieldName ?? 'Name'}</span>
        <input value={v.name} onChange={e => onChange({ ...v, name: e.target.value })} maxLength={80} required />
      </label>
      <label className={styles.field}>
        <span>{tc.fieldBirthYear ?? 'Birth year'}</span>
        <input type="number" min={1990} max={new Date().getFullYear()}
               value={v.birthYear} onChange={e => onChange({ ...v, birthYear: e.target.value === '' ? '' : Number(e.target.value) })} />
      </label>
      <label className={styles.field}>
        <span>{tc.fieldGender ?? 'Gender'}</span>
        <select value={v.gender} onChange={e => onChange({ ...v, gender: e.target.value as ChildFormValues['gender'] })}>
          <option value="">{tc.genderUnspecified ?? '— unspecified —'}</option>
          <option value="boy">{tc.genderBoy ?? 'Boy'}</option>
          <option value="girl">{tc.genderGirl ?? 'Girl'}</option>
          <option value="other">{tc.genderOther ?? 'Other'}</option>
        </select>
      </label>
      <label className={styles.field}>
        <span>{tc.fieldLanguage ?? 'Tale language'}</span>
        <select value={v.preferredLanguage}
                onChange={e => onChange({ ...v, preferredLanguage: e.target.value as ChildFormValues['preferredLanguage'] })}>
          <option value="uk">{tc.langUk ?? 'Ukrainian'}</option>
          <option value="en">{tc.langEn ?? 'English'}</option>
          <option value="bilingual">{tc.langBilingual ?? 'Bilingual (UK fallback for now)'}</option>
        </select>
        <small>{tc.languageHelper ?? 'Language of generated tales — UI language stays as you chose at top.'}</small>
      </label>
      <div className={styles.field}>
        <span>{tc.fieldInterests ?? 'Interests'}</span>
        <div className={styles.chipRow}>
          {v.interests.map(x => (
            <span key={x} className={styles.chip}>
              {x}
              <button type="button" aria-label="remove" onClick={() => onChange({ ...v, interests: v.interests.filter(i => i !== x) })}>×</button>
            </span>
          ))}
        </div>
        <div className={styles.chipInput}>
          <input value={interestDraft} placeholder={tc.interestsPlaceholder ?? 'e.g. dragons, cats, space'}
                 onChange={e => setInterestDraft(e.target.value)}
                 onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addInterest() } }} />
          <button type="button" onClick={addInterest} disabled={!interestDraft.trim() || v.interests.length >= 10}>+</button>
        </div>
      </div>
    </>
  )
}

export interface ChildProfileFormProps {
  initial?: ChildProfileDto
  onSubmit: (v: ChildFormValues) => Promise<void>
  submitLabel: string
}

export function ChildProfileForm({ initial, onSubmit, submitLabel }: ChildProfileFormProps) {
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const [v, setV] = useState<ChildFormValues>({
    name: initial?.name ?? '',
    birthYear: initial?.birthYear ?? '',
    gender: (initial?.gender as ChildFormValues['gender']) ?? '',
    preferredLanguage: (initial?.preferredLanguage as ChildFormValues['preferredLanguage']) ?? 'uk',
    interests: initial?.interests ?? [],
  })
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true); setErr(null)
    try { await onSubmit(v) }
    catch (e: any) { setErr(e?.message ?? tc.saveError ?? 'Could not save') }
    finally { setBusy(false) }
  }

  return (
    <form onSubmit={submit} className={styles.form}>
      <ChildProfileFields value={v} onChange={setV} />
      {err && <p className={styles.error}>{err}</p>}
      <button type="submit" className={styles.submit} disabled={busy || !v.name.trim()}>{submitLabel}</button>
    </form>
  )
}
