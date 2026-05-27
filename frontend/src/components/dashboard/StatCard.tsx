import styles from './StatCard.module.css'

export interface StatCardProps {
  label: string
  value: number
}

export function StatCard({ label, value }: StatCardProps) {
  return (
    <div className={styles.card}>
      <div className={styles.value}>{value}</div>
      <div className={styles.label}>{label}</div>
    </div>
  )
}
