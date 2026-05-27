import { Link } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import styles from './QuickLinks.module.css'

export function QuickLinks() {
  const { t } = useLocale()
  const td = (t as any).dashboard ?? {}
  return (
    <div className={styles.grid}>
      <Link to="/children" className={styles.link}>{td.manageChildren ?? 'Manage children'}</Link>
      <Link to="/settings" className={styles.link}>{td.bedtime ?? 'Bedtime'}</Link>
      <Link to="/pricing" className={styles.link}>{td.billing ?? 'Billing'}</Link>
    </div>
  )
}
