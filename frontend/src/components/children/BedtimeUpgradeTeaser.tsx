import { useNavigate } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import styles from './BedtimeUpgradeTeaser.module.css'

export function BedtimeUpgradeTeaser() {
  const { t } = useLocale()
  const tb = (t as any).children?.bedtime ?? {}
  const navigate = useNavigate()
  return (
    <div className={styles.card}>
      <h3>{tb.upgradeTitle ?? 'Nightly bedtime tales'}</h3>
      <p>{tb.upgradeBody ?? 'Get a fresh tale emailed every night at your chosen bedtime. Paid feature.'}</p>
      <button type="button" onClick={() => navigate('/pricing')}>
        {tb.upgradeCta ?? (t as any).common?.upgrade ?? 'Upgrade'}
      </button>
    </div>
  )
}
