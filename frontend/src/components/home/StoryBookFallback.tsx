import { useLocale } from '../../lib/LocaleContext'
import styles from './StoryBook.module.css'

export function StoryBookFallback() {
  const { t } = useLocale()
  return (
    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 24, maxWidth: 560 }}>
      {t.howItWorks.steps.map((step, i) => (
        <li key={i} style={{ display: 'flex', gap: 16 }}>
          <div className={styles.pageNumber} style={{ position: 'static', opacity: 0.7 }}>{step.num}</div>
          <div>
            <div className={styles.stepLabel}>{step.stepLabel}</div>
            <h3 className={styles.stepTitle}>{step.title}</h3>
            <p className={styles.stepDesc}>{step.desc}</p>
          </div>
        </li>
      ))}
    </ul>
  )
}
