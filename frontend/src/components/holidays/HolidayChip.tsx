import { useEffect, useState } from 'react'
import { useChildren } from '../../lib/ChildrenContext'
import { useLocale } from '../../lib/LocaleContext'
import { holidays as holidaysApi } from '../../lib/apiClient'
import type { HolidayDto } from '@kazka/shared'
import styles from './HolidayChip.module.css'

export interface HolidayChipProps {
  onApply: (themeText: string) => void
}

export function HolidayChip({ onApply }: HolidayChipProps) {
  const { active } = useChildren()
  const { t } = useLocale()
  const tc = (t as any).holidayChip ?? {}
  const [holiday, setHoliday] = useState<HolidayDto | null>(null)
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    if (!active) { setHoliday(null); return }
    const lang = active.preferredLanguage === 'en' ? 'en' : 'uk'
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Kyiv'
    holidaysApi.today(tz, lang)
      .then(h => {
        if (!h) { setHoliday(null); return }
        const dateKey = h.date.slice(0, 10)
        const lsKey = `kazka.dismissedHoliday.${h.id}-${dateKey}`
        if (localStorage.getItem(lsKey)) { setDismissed(true); return }
        setHoliday(h)
      })
      .catch(() => setHoliday(null))
  }, [active])

  if (!holiday || dismissed) return null

  function dismissForToday() {
    if (!holiday) return
    const key = `kazka.dismissedHoliday.${holiday.id}-${holiday.date.slice(0, 10)}`
    localStorage.setItem(key, '1')
  }

  function apply() {
    onApply(holiday!.suggestedTheme)
    dismissForToday()
    setDismissed(true)
  }

  function dismiss() {
    dismissForToday()
    setDismissed(true)
  }

  return (
    <div className={styles.chip}>
      <span className={styles.label}>
        🎄 {tc.prompt ?? 'Today is'} {holiday.name}.
      </span>
      <button type="button" className={styles.apply} onClick={apply}>
        {tc.apply ?? 'Try it'}
      </button>
      <button type="button" className={styles.dismiss} onClick={dismiss} aria-label={tc.dismiss ?? 'Dismiss'}>×</button>
    </div>
  )
}
