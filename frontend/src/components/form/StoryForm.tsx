import { useState } from 'react'
import type { FormEvent } from 'react'
import { TagInput } from './TagInput'
import { useLocale } from '../../lib/LocaleContext'
import type { GenerationRequest } from '../../lib/types'
import styles from './StoryForm.module.css'

interface StoryFormProps {
  onSubmit: (req: GenerationRequest) => void
  loading: boolean
  inModal?: boolean
}

export function StoryForm({ onSubmit, loading, inModal }: StoryFormProps) {
  const { t, lang } = useLocale()
  const [theme, setTheme] = useState('')
  const [characters, setCharacters] = useState<string[]>([])
  const [ageGroup, setAgeGroup] = useState<GenerationRequest['ageGroup']>('6-8')
  const [length, setLength] = useState<GenerationRequest['length']>('medium')
  const [language, setLanguage] = useState<'uk' | 'en'>(lang)

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!theme.trim() || characters.length === 0) return
    onSubmit({ theme: theme.trim(), characters, ageGroup, length, language })
  }

  return (
    <form className={`${styles.form} ${inModal ? styles.formInModal : ''}`} onSubmit={handleSubmit}>
      <h2 className={styles.title}>{t.form.title}</h2>

      <div className={styles.field}>
        <label htmlFor="theme" className={styles.label}>{t.form.theme}</label>
        <input
          id="theme"
          type="text"
          className={styles.input}
          value={theme}
          onChange={e => setTheme(e.target.value)}
          placeholder={t.form.themePlaceholder}
          required
        />
      </div>

      <div className={styles.field}>
        <label htmlFor="characters" className={styles.label}>{t.form.characters}</label>
        <TagInput
          id="characters"
          value={characters}
          onChange={setCharacters}
          placeholder={t.form.charactersPlaceholder}
        />
      </div>

      <div className={styles.row}>
        <div className={styles.field}>
          <label htmlFor="ageGroup" className={styles.label}>{t.form.ageGroup}</label>
          <select
            id="ageGroup"
            className={styles.select}
            value={ageGroup}
            onChange={e => setAgeGroup(e.target.value as GenerationRequest['ageGroup'])}
          >
            {(Object.entries(t.form.ageGroups) as [GenerationRequest['ageGroup'], string][]).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>

        <div className={styles.field}>
          <label htmlFor="length" className={styles.label}>{t.form.length}</label>
          <select
            id="length"
            className={styles.select}
            value={length}
            onChange={e => setLength(e.target.value as GenerationRequest['length'])}
          >
            {(Object.entries(t.form.lengths) as [GenerationRequest['length'], string][]).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>

        <div className={styles.field}>
          <label htmlFor="language" className={styles.label}>{t.form.language}</label>
          <select
            id="language"
            className={styles.select}
            value={language}
            onChange={e => setLanguage(e.target.value as 'uk' | 'en')}
          >
            {(Object.entries(t.form.languages) as ['uk' | 'en', string][]).map(([k, v]) => (
              <option key={k} value={k}>{v}</option>
            ))}
          </select>
        </div>
      </div>

      <button
        type="submit"
        className={styles.submit}
        disabled={loading || !theme.trim() || characters.length === 0}
      >
        {loading ? t.form.generating : t.form.submit}
      </button>
    </form>
  )
}
