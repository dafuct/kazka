import { useState } from 'react'
import styles from './TimezoneSelect.module.css'

const COMMON_ZONES = [
  'Europe/Kyiv',
  'Europe/Warsaw',
  'Europe/Berlin',
  'Europe/Prague',
  'Europe/London',
  'America/New_York',
  'America/Los_Angeles',
  'America/Toronto',
] as const

export function TimezoneSelect({ value, onChange }: { value: string; onChange: (tz: string) => void }) {
  const inList = (COMMON_ZONES as readonly string[]).includes(value)
  const [mode, setMode] = useState<'list' | 'custom'>(inList || !value ? 'list' : 'custom')

  if (mode === 'custom') {
    return (
      <div className={styles.row}>
        <input
          className={styles.input}
          value={value}
          placeholder="Continent/City"
          onChange={e => onChange(e.target.value)}
        />
        <button type="button" className={styles.linkBtn} onClick={() => { setMode('list'); onChange('Europe/Kyiv') }}>
          ← list
        </button>
      </div>
    )
  }
  return (
    <select
      className={styles.select}
      value={inList ? value : COMMON_ZONES[0]}
      onChange={e => {
        if (e.target.value === '__other__') { setMode('custom'); onChange('') }
        else onChange(e.target.value)
      }}
    >
      {COMMON_ZONES.map(tz => (<option key={tz} value={tz}>{tz}</option>))}
      <option value="__other__">Other…</option>
    </select>
  )
}
