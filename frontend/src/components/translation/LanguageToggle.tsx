import { useState } from 'react'
import { translation } from '../../lib/apiClient'
import { useLocale } from '../../lib/LocaleContext'
import type { Story } from '@kazka/shared'
import styles from './LanguageToggle.module.css'

export interface LanguageToggleProps {
  story: Story
  active: 'original' | 'translated'
  onSwitch: (active: 'original' | 'translated', updatedStory?: Story) => void
}

export function LanguageToggle({ story, active, onSwitch }: LanguageToggleProps) {
  const { t } = useLocale()
  const tt = (t as any).translate ?? {}
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const original = story.language ?? 'uk'
  const translated = story.translatedLanguage ?? (original === 'uk' ? 'en' : 'uk')

  async function pickTranslated() {
    if (active === 'translated') return
    if (story.translatedContent && story.translatedLanguage === translated) {
      onSwitch('translated')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const updated = await translation.translate(story.id ?? '', translated as 'uk' | 'en')
      onSwitch('translated', updated)
    } catch (e: any) {
      setError(e?.message ?? (tt.error ?? 'Translation failed'))
    } finally {
      setBusy(false)
    }
  }

  function pickOriginal() {
    if (active === 'original') return
    onSwitch('original')
  }

  return (
    <div className={styles.wrap}>
      <button
        type="button"
        className={active === 'original' ? styles.pillActive : styles.pill}
        onClick={pickOriginal}
        disabled={busy}
      >
        {labelFor(original)}
      </button>
      <button
        type="button"
        className={active === 'translated' ? styles.pillActive : styles.pill}
        onClick={pickTranslated}
        disabled={busy}
      >
        {busy ? (tt.translating ?? 'Translating…') : labelFor(translated)}
      </button>
      {error && <span className={styles.error}>{error}</span>}
    </div>
  )
}

function labelFor(code: string): string {
  return code === 'uk' ? '🇺🇦 UA' : '🇬🇧 EN'
}
