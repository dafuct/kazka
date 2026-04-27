import { useLocale } from '../../lib/LocaleContext'
import styles from './Features.module.css'

const icons = ['🔒', '🇺🇦', '✨']

export function Features() {
  const { t } = useLocale()

  return (
    <section className={styles.section}>
      <h2 className={styles.heading}>{t.features.title}</h2>
      <ul className={styles.grid}>
        {t.features.items.map((item, i) => (
          <li key={i} className={styles.card}>
            <span className={styles.icon}>{icons[i]}</span>
            <h3 className={styles.cardTitle}>{item.title}</h3>
            <p className={styles.cardDesc}>{item.desc}</p>
          </li>
        ))}
      </ul>
    </section>
  )
}
