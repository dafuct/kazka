import { useEffect, useState } from 'react'
import { useBilling } from '../../lib/BillingContext'
import { useLocale } from '../../lib/LocaleContext'
import { charactersApi, extraction } from '../../lib/apiClient'
import type { ExtractedCandidateDto } from '@kazka/shared'
import styles from './ExtractedCharactersPanel.module.css'

type ExtractionStatus = 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED' | 'SKIPPED'

export interface ExtractedCharactersPanelProps {
  storyId: string
  childProfileId: string
  extractionStatus: ExtractionStatus
  language?: string
  onConfirmed: () => void
}

export function ExtractedCharactersPanel(props: ExtractedCharactersPanelProps) {
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const { isPro } = useBilling()
  const [candidates, setCandidates] = useState<ExtractedCandidateDto[]>([])
  const [picked, setPicked] = useState<Set<number>>(new Set())
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)

  useEffect(() => {
    if (props.extractionStatus !== 'DONE') return
    extraction.candidates(props.storyId, props.language).then(rows => {
      setCandidates(rows)
      setPicked(new Set(rows.map((_, i) => i)))
    }).catch(() => setCandidates([]))
  }, [props.storyId, props.extractionStatus, props.language])

  const statusLabels: Record<ExtractionStatus, string> = {
    PENDING:  tc.extractionStatus?.PENDING  ?? 'Preparing character analysis…',
    RUNNING:  tc.extractionStatus?.RUNNING  ?? 'Detecting characters from the tale…',
    DONE:     tc.extractionStatus?.DONE     ?? 'Done',
    FAILED:   tc.extractionStatus?.FAILED   ?? 'Could not detect characters — try again later.',
    SKIPPED:  tc.extractionStatus?.SKIPPED  ?? '',
  }

  if (props.extractionStatus !== 'DONE') {
    const label = statusLabels[props.extractionStatus]
    return label ? <p className={styles.status}>{label}</p> : null
  }
  if (done) return <p className={styles.status}>{tc.charactersSaved ?? 'Saved to library 🎉'}</p>
  if (candidates.length === 0) return null

  async function confirm() {
    setBusy(true)
    try {
      const chosen = candidates.filter((_, i) => picked.has(i))
      await charactersApi.confirm(props.childProfileId, {
        storyId: props.storyId,
        candidates: chosen,
      })
      setDone(true)
      props.onConfirmed()
    } finally { setBusy(false) }
  }

  return (
    <section className={styles.panel}>
      <h3>{tc.saveTheseTitle ?? 'Save these characters?'}</h3>
      {!isPro && <p className={styles.upgradeNote}>{tc.upgradeToSave ?? 'Upgrade to save characters to your library.'}</p>}
      <ul className={styles.list}>
        {candidates.map((c, i) => (
          <li key={i}>
            <label>
              <input type="checkbox" disabled={!isPro}
                     checked={picked.has(i)}
                     onChange={() => {
                       const n = new Set(picked)
                       if (n.has(i)) n.delete(i); else n.add(i)
                       setPicked(n)
                     }} />
              <strong>{c.name}</strong> <small>({c.kind})</small> — {c.description}
            </label>
          </li>
        ))}
      </ul>
      <button onClick={confirm} disabled={!isPro || busy || picked.size === 0}>
        {tc.saveSelected ?? 'Save selected'}
      </button>
    </section>
  )
}
