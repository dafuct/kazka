import styles from './PlanCard.module.css'
import type { ReactNode } from 'react'

interface Props {
  name: string
  price: string
  pricePeriod?: string
  bullets: string[]
  highlighted?: boolean
  badge?: string
  cta?: ReactNode
}

export function PlanCard({ name, price, pricePeriod, bullets, highlighted, badge, cta }: Props) {
  return (
    <div className={`${styles.card} ${highlighted ? styles.highlighted : ''}`}>
      {badge && <div className={styles.badge}>{badge}</div>}
      <h3 className={styles.name}>{name}</h3>
      <div className={styles.priceRow}>
        <span className={styles.price}>{price}</span>
        {pricePeriod && <span className={styles.period}>{pricePeriod}</span>}
      </div>
      <ul className={styles.bullets}>
        {bullets.map((b, i) => <li key={i}>{b}</li>)}
      </ul>
      {cta && <div className={styles.cta}>{cta}</div>}
    </div>
  )
}
