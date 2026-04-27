import { useLocale } from '../../lib/LocaleContext'
import styles from './HowItWorks.module.css'

export function HowItWorks() {
  const { t } = useLocale()

  return (
    <section className={styles.section}>
      <h2 className={styles.heading}>{t.howItWorks.title}</h2>
      <ol className={styles.steps}>
        {t.howItWorks.steps.map((step, i) => (
          <li key={i} className={styles.step}>
            <span className={styles.num}>{i + 1}</span>
            <div>
              <h3 className={styles.stepTitle}>{step.title}</h3>
              <p className={styles.stepDesc}>{step.desc}</p>
            </div>
          </li>
        ))}
      </ol>
    </section>
  )
}
