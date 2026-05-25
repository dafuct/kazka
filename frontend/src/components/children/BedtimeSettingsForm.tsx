import { useEffect, useState } from 'react'
import { useLocale } from '../../lib/LocaleContext'
import { useAuth } from '../../lib/AuthContext'
import { children as childrenApi } from '../../lib/apiClient'
import type { BedtimeScheduleDto } from '../../lib/types'
import { TimezoneSelect } from './TimezoneSelect'
import styles from './BedtimeSettingsForm.module.css'

export function BedtimeSettingsForm({ childId }: { childId: string }) {
  const { t } = useLocale()
  const { user } = useAuth()
  const tb = (t as any).children?.bedtime ?? {}
  const [loading, setLoading] = useState(true)
  const [enabled, setEnabled] = useState(false)
  const [localTime, setLocalTime] = useState('20:30')
  const [timezone, setTimezone] = useState('Europe/Kyiv')
  const [themesDraft, setThemesDraft] = useState('')
  const [themes, setThemes] = useState<string[]>([])
  const [busy, setBusy] = useState(false)
  const [savedMsg, setSavedMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const [failedAt, setFailedAt] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    childrenApi.getBedtime(childId)
      .then((s: BedtimeScheduleDto) => {
        setEnabled(s.enabled)
        setLocalTime(s.localTime)
        setTimezone(s.timezone)
        setThemes(s.themes ?? [])
        setFailedAt(s.failedAt ?? null)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [childId])

  function addTheme() {
    const x = themesDraft.trim()
    if (!x || themes.includes(x) || themes.length >= 10) return
    setThemes([...themes, x])
    setThemesDraft('')
  }

  async function save(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true); setSavedMsg(null); setErr(null)
    try {
      await childrenApi.updateBedtime(childId, { enabled, localTime, timezone, themes })
      setSavedMsg(tb.savedToast ?? 'Saved')
    } catch (e: any) {
      setErr(e?.message ?? tb.saveError ?? 'Could not save')
    } finally { setBusy(false) }
  }

  if (loading) return <p>{(t as any).common?.loading ?? 'Loading…'}</p>

  return (
    <form onSubmit={save} className={styles.form}>
      {failedAt && (
        <div className={styles.failNotice}>
          <strong>{tb.failedNoticeTitle ?? "Last night's tale didn't generate."}</strong>
          <p>{tb.failedNoticeBody ?? "We'll try again tonight."}</p>
        </div>
      )}
      <label className={styles.field}>
        <input type="checkbox" checked={enabled} onChange={e => setEnabled(e.target.checked)} />
        <span>{tb.enableLabel ?? 'Send a tale every night'}</span>
      </label>
      <label className={styles.field}>
        <span>{tb.localTimeLabel ?? 'Local bedtime'}</span>
        <input type="time" value={localTime} onChange={e => setLocalTime(e.target.value)} />
      </label>
      <label className={styles.field}>
        <span>{tb.timezoneLabel ?? 'Timezone'}</span>
        <TimezoneSelect value={timezone} onChange={setTimezone} />
      </label>
      <div className={styles.field}>
        <span>{tb.themesLabel ?? 'Themes (optional)'}</span>
        <div className={styles.chipRow}>
          {themes.map(x => (
            <span key={x} className={styles.chip}>
              {x}
              <button type="button" onClick={() => setThemes(themes.filter(t => t !== x))}>×</button>
            </span>
          ))}
        </div>
        <div className={styles.chipInput}>
          <input value={themesDraft}
                 placeholder={tb.themesInheritOption ?? 'Leave empty to use the profile interests'}
                 onChange={e => setThemesDraft(e.target.value)}
                 onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addTheme() } }} />
          <button type="button" onClick={addTheme} disabled={!themesDraft.trim() || themes.length >= 10}>+</button>
        </div>
      </div>
      <p className={styles.helper}>
        {tb.helperWeWillEmail
          ? tb.helperWeWillEmail({ email: user?.email ?? '', time: localTime, tz: timezone })
          : `We'll email a fresh tale to ${user?.email ?? 'your inbox'} each night at ${localTime} ${timezone}.`}
      </p>
      {err && <p className={styles.error}>{err}</p>}
      {savedMsg && <p className={styles.success}>{savedMsg}</p>}
      <button type="submit" className={styles.submit} disabled={busy}>
        {tb.saveCta ?? (t as any).common?.save ?? 'Save'}
      </button>
    </form>
  )
}
