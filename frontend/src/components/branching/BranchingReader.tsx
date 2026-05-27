import { useState } from 'react'
import { branching } from '../../lib/apiClient'
import { useLocale } from '../../lib/LocaleContext'
import type { Story } from '@kazka/shared'
import { BranchingChoiceButtons } from './BranchingChoiceButtons'
import styles from './BranchingReader.module.css'

export interface BranchingReaderProps {
  story: Story
  onComplete: () => void
}

export function BranchingReader({ story: initial, onComplete }: BranchingReaderProps) {
  const { t } = useLocale()
  const tb = (t as any).branching ?? {}
  const [content, setContent] = useState(initial.content ?? '')
  const [choices, setChoices] = useState(initial.pendingChoices ?? [])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function pickChoice(choiceId: string) {
    setBusy(true)
    setError(null)
    try {
      const resp = await branching.choose(initial.id, choiceId)
      setContent(resp.content ?? '')
      if (resp.isFinal) {
        onComplete()
      } else {
        setChoices(resp.choices ?? [])
      }
    } catch (e: any) {
      setError(e?.message ?? (tb.errorRetry ?? 'Something went wrong. Please refresh.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className={styles.reader}>
      <article className={styles.content}>
        {content.split('\n').map((line: string, i: number) => (
          line.trim() ? <p key={i}>{line}</p> : null
        ))}
      </article>
      {error && <p className={styles.error}>{error}</p>}
      {busy && <p className={styles.loading}>{tb.loading ?? 'Generating next part…'}</p>}
      {!busy && choices.length > 0 && (
        <BranchingChoiceButtons
          choices={choices}
          onPick={pickChoice}
          prompt={tb.choicePrompt ?? 'What happens next?'}
        />
      )}
    </div>
  )
}
