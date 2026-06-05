import styles from './ProviderSelector.module.css'
import { useLocale } from '../../lib/LocaleContext'
import type { ProviderName } from '../../lib/types'

interface Props {
  isUkraine: boolean
  loading?: boolean
  onSubscribe: (provider: ProviderName) => void
}

export function ProviderSelector({ isUkraine, loading, onSubscribe }: Props) {
  const { t } = useLocale()

  if (!isUkraine) {
    return (
      <div className={styles.wrap}>
        <p className={styles.comingSoon}>{t.pricing.comingSoonForCountry}</p>
      </div>
    )
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.label}>{t.pricing.payWith}</div>
      <div className={styles.buttons}>
        <button className={styles.btn} disabled={loading}
                onClick={() => onSubscribe('monobank')}>
          💳 {t.pricing.monobankRecurring}
        </button>
      </div>
      <p className={styles.disclosure}>{t.pricing.paymentMethods}</p>
      <p className={styles.disclosure}>{t.pricing.autoRenewalDisclosure}</p>
    </div>
  )
}
