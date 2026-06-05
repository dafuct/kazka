import { useState } from 'react'
import styles from './ProviderSelector.module.css'
import { useLocale } from '../../lib/LocaleContext'
import type { ProviderName } from '../../lib/types'

interface Props {
  country: string
  isUkraine: boolean
  loading?: boolean
  onSubscribe: (provider: ProviderName) => void
  onCountryChange: (country: string | null) => void
}

const NON_UA_OPTIONS = ['US', 'GB', 'DE', 'FR', 'PL', 'CA', 'AU']

export function ProviderSelector({ country, isUkraine, loading, onSubscribe, onCountryChange }: Props) {
  const { t } = useLocale()
  const [editingCountry, setEditingCountry] = useState(false)

  return (
    <div className={styles.wrap}>
      <div className={styles.label}>{t.pricing.payWith}</div>
      <div className={styles.buttons}>
        {isUkraine ? (
          <button className={styles.btn} disabled={loading}
                  onClick={() => onSubscribe('monobank')}>
            💳 {t.pricing.monobankRecurring}
          </button>
        ) : (
          <button className={styles.btn} disabled={loading}
                  onClick={() => onSubscribe('paypro')}>
            💳 {t.pricing.paypro}
          </button>
        )}
      </div>
      {isUkraine && (
        <>
          <p className={styles.disclosure}>{t.pricing.paymentMethods}</p>
          <p className={styles.disclosure}>{t.pricing.autoRenewalDisclosure}</p>
        </>
      )}
      <div className={styles.geoRow}>
        <span>{t.pricing.detectedCountry.replace('{country}', country)}</span>
        <button className={styles.linkBtn} onClick={() => setEditingCountry(v => !v)}>
          {t.pricing.changeCountry}
        </button>
      </div>
      {editingCountry && (
        <div className={styles.picker}>
          <button className={styles.pickBtn}
                  onClick={() => { onCountryChange('UA'); setEditingCountry(false) }}>
            🇺🇦 Ukraine
          </button>
          {NON_UA_OPTIONS.map(c => (
            <button key={c} className={styles.pickBtn}
                    onClick={() => { onCountryChange(c); setEditingCountry(false) }}>
              {c}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
