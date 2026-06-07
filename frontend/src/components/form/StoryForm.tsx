import { useState, useEffect } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { TagInput } from './TagInput'
import { CharacterPicker } from '../children/CharacterPicker'
import { useLocale } from '../../lib/LocaleContext'
import { useAuth } from '../../lib/AuthContext'
import { useChildren } from '../../lib/ChildrenContext'
import type { GenerationRequest } from '../../lib/types'
import { branching } from '../../lib/apiClient'
import styles from './StoryForm.module.css'

interface StoryFormProps {
  onSubmit: (req: GenerationRequest) => void
  loading: boolean
  inModal?: boolean
}

export function StoryForm({ onSubmit, loading, inModal }: StoryFormProps) {
  const navigate = useNavigate()
  const { t, lang } = useLocale()
  const { user } = useAuth()
  const { active } = useChildren()
  const isSuspended = !!user?.suspended
  const [theme, setTheme] = useState(() => {
    const suggested = localStorage.getItem('kazka.suggestedTheme')
    if (suggested) localStorage.removeItem('kazka.suggestedTheme')
    return suggested ?? ''
  })
  const [characters, setCharacters] = useState<string[]>([])
  const [ageGroup, setAgeGroup] = useState<GenerationRequest['ageGroup']>('6-8')
  const [length, setLength] = useState<GenerationRequest['length']>('medium')
  const [language, setLanguage] = useState<'uk' | 'en'>(lang)
  const [includeCharacterIds, setIncludeCharacterIds] = useState<string[]>([])
  const [isBranching, setIsBranching] = useState(false)

  // Prefill language from the active child's preference; bilingual → 'uk'
  useEffect(() => {
    if (!active) return
    const pref = active.preferredLanguage
    setLanguage(pref === 'en' ? 'en' : 'uk')
  }, [active])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!theme.trim() || characters.length === 0 || !active) return

    if (isBranching) {
      try {
        const resp = await branching.start({
          theme: theme.trim(),
          characters,
          ageGroup,
          length,
          language,
          childProfileId: active.id,
          includeCharacterIds: includeCharacterIds.length > 0 ? includeCharacterIds : undefined,
        })
        navigate(`/stories/${resp.storyId}`)
      } catch (err: any) {
        // 402 → /pricing redirect is handled automatically by apiClient
        console.error('Could not start branching tale', err)
      }
      return
    }

    onSubmit({
      theme: theme.trim(),
      characters,
      ageGroup,
      length,
      language,
      childProfileId: active.id,
      includeCharacterIds: includeCharacterIds.length > 0 ? includeCharacterIds : undefined,
    })
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

      <div className={styles.field}>
        <CharacterPicker selected={includeCharacterIds} onChange={setIncludeCharacterIds} />
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

      <label className={`${styles.magicToggle} ${isBranching ? styles.magicToggleOn : ''}`}>
        <input
          type="checkbox"
          className={styles.magicToggleInput}
          checked={isBranching}
          onChange={e => setIsBranching(e.target.checked)}
        />
        <span className={styles.magicToggleIcon} aria-hidden="true">✨</span>
        <span className={styles.magicToggleLabel}>
          {(t as any).branching?.formToggle ?? 'Branching tale'}
        </span>
      </label>

      <button
        type="submit"
        className={styles.submit}
        disabled={loading || isSuspended || !theme.trim() || characters.length === 0 || !active}
        title={isSuspended ? t.moderation.formDisabledSuspended : undefined}
      >
        {loading ? t.form.generating : t.form.submit}
      </button>
    </form>
  )
}
