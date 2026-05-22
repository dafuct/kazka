import styles from './PlanToggle.module.css'
import { useLocale } from '../../lib/LocaleContext'

export type Period = 'monthly' | 'yearly'

interface Props {
  value: Period
  onChange: (p: Period) => void
}

export function PlanToggle({ value, onChange }: Props) {
  const { t } = useLocale()
  return (
    <div className={styles.toggle} role="tablist">
      <button role="tab" aria-selected={value === 'monthly'}
              className={`${styles.opt} ${value === 'monthly' ? styles.active : ''}`}
              onClick={() => onChange('monthly')}>
        {t.pricing.monthly}
      </button>
      <button role="tab" aria-selected={value === 'yearly'}
              className={`${styles.opt} ${value === 'yearly' ? styles.active : ''}`}
              onClick={() => onChange('yearly')}>
        {t.pricing.yearly}
        <span className={styles.badge}>{t.pricing.saveYearly}</span>
      </button>
    </div>
  )
}
