import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLocale } from '../lib/LocaleContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { showcase } from '../lib/apiClient'
import type { ShowcaseStoryDto } from '../lib/apiClient'
import styles from './ShowcasePage.module.css'

/** Repeated, prominent "sign up to create your own tale" CTA. */
export function ShowcaseCta() {
  const { t } = useLocale()
  const { openAuth } = useAuthModal()
  const ts = t.showcase
  return (
    <section className={styles.cta}>
      <h2 className={styles.ctaTitle}>{ts.cta.title}</h2>
      <p className={styles.ctaSub}>{ts.cta.subtitle}</p>
      <button type="button" className={styles.ctaButton} onClick={() => openAuth('signUp')}>
        {ts.cta.button}
      </button>
    </section>
  )
}

export function ShowcasePage() {
  const { t } = useLocale()
  const ts = t.showcase
  const [tales, setTales] = useState<ShowcaseStoryDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let cancelled = false
    showcase.list()
      .then(list => { if (!cancelled) setTales(list) })
      .catch(() => { if (!cancelled) setError(true) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  return (
    <div className={styles.page}>
      <div className={styles.inner}>
        <div className={styles.pageHeader}>
          <div className={styles.label}>{ts.label}</div>
          <h1 className={styles.heading}>{ts.heading}</h1>
          <p className={styles.intro}>{ts.intro}</p>
        </div>

        {loading && <p className={styles.msg}>{ts.loading}</p>}
        {!loading && error && <p className={styles.msg}>{ts.error}</p>}
        {!loading && !error && tales.length === 0 && (
          <p className={styles.msg}>{ts.empty}</p>
        )}

        {!loading && !error && tales.length > 0 && (
          <ul className={styles.grid}>
            {tales.map(tale => {
              const cover = tale.panels?.[0]?.imageUrl
              return (
                <li key={tale.id}>
                  <Link to={`/showcase/${tale.id}`} className={styles.card}>
                    {cover ? (
                      <img className={styles.cover} src={cover} alt={tale.title} loading="lazy" />
                    ) : (
                      <div className={styles.coverFallback} aria-hidden="true" />
                    )}
                    <div className={styles.cardBody}>
                      <h2 className={styles.cardTitle}>{tale.title}</h2>
                      {tale.theme && <p className={styles.cardTheme}>{tale.theme}</p>}
                    </div>
                  </Link>
                </li>
              )
            })}
          </ul>
        )}

        <ShowcaseCta />
      </div>
    </div>
  )
}
