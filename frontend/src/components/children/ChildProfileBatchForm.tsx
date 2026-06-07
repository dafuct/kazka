import { useState } from 'react'
import { useLocale } from '../../lib/LocaleContext'
import type { CreateChildProfileRequest } from '../../lib/types'
import { ChildProfileFields, emptyChildFormValues } from './ChildProfileForm'
import type { ChildFormValues } from './ChildProfileForm'
import formStyles from './ChildProfileForm.module.css'
import styles from './ChildProfileBatchForm.module.css'

const MAX_ROWS = 20

function toRequest(v: ChildFormValues): CreateChildProfileRequest {
  return {
    name: v.name.trim(),
    birthYear: v.birthYear === '' ? undefined : (v.birthYear as number),
    gender: v.gender || undefined,
    preferredLanguage: v.preferredLanguage,
    interests: v.interests,
  }
}

export interface ChildProfileBatchFormProps {
  /** One submit for all rows; resolves once the batch create succeeds. */
  onSubmit: (children: CreateChildProfileRequest[]) => Promise<void>
  submitLabel: string
}

/**
 * Repeatable-rows create form: lets the user add several children before a
 * single submit that fires one batch request. Reuses {@link ChildProfileFields}
 * (and ChildProfileForm.module.css) for each row so the field markup/styling
 * matches the single-child form. The single-child edit flow is untouched.
 */
export function ChildProfileBatchForm({ onSubmit, submitLabel }: ChildProfileBatchFormProps) {
  const { t } = useLocale()
  const tc = (t as any).children ?? {}
  const [rows, setRows] = useState<ChildFormValues[]>([emptyChildFormValues()])
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  function updateRow(index: number, value: ChildFormValues) {
    setRows(prev => prev.map((r, i) => (i === index ? value : r)))
  }

  function addRow() {
    setRows(prev => (prev.length >= MAX_ROWS ? prev : [...prev, emptyChildFormValues()]))
  }

  function removeRow(index: number) {
    setRows(prev => (prev.length <= 1 ? prev : prev.filter((_, i) => i !== index)))
  }

  const allNamed = rows.every(r => r.name.trim().length > 0)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!allNamed) return
    setBusy(true); setErr(null)
    try {
      await onSubmit(rows.map(toRequest))
    } catch (e: any) {
      setErr(e?.message ?? tc.saveError ?? 'Could not save')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className={styles.batch}>
      {rows.map((row, i) => (
        <fieldset key={i} className={styles.row}>
          <legend className={styles.legend}>
            <span>{(tc.childN ?? ((n: number) => `Child ${n}`))(i + 1)}</span>
            {rows.length > 1 && (
              <button
                type="button"
                className={styles.removeBtn}
                onClick={() => removeRow(i)}
              >
                {tc.removeChild ?? 'Remove'}
              </button>
            )}
          </legend>
          <div className={formStyles.form}>
            <ChildProfileFields value={row} onChange={v => updateRow(i, v)} />
          </div>
        </fieldset>
      ))}

      <button
        type="button"
        className={styles.addBtn}
        onClick={addRow}
        disabled={rows.length >= MAX_ROWS}
      >
        {tc.addAnotherChild ?? '+ Add another child'}
      </button>

      {err && <p className={formStyles.error}>{err}</p>}

      <button
        type="submit"
        className={formStyles.submit}
        disabled={busy || !allNamed}
      >
        {submitLabel}
      </button>
    </form>
  )
}
