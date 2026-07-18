import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { TapedCard } from '../components/taped/TapedCard'
import { useLocale } from '../lib/LocaleContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { useAuth } from '../lib/AuthContext'
import { showcase } from '../lib/apiClient'
import type { ShowcaseStoryDto } from '../lib/apiClient'
import styles from './ShowcasePage.module.css'

/** Prominent CTA: logged-out → sign up; logged-in → the create page. */
export function ShowcaseCta() {
  const { t } = useLocale()
  const { openAuth } = useAuthModal()
  const { user } = useAuth()
  const navigate = useNavigate()
  const ts = t.showcase
  const handleClick = () => {
    if (!user) openAuth('signUp')
    else navigate('/create')
  }
  return (
    <section className={`band ${styles.cta}`}>
      <h2>{ts.cta.title}</h2>
      <p>{ts.cta.subtitle}</p>
      <button type="button" className="btn btn-primary btn-lg" onClick={handleClick}>
        {ts.cta.button}
      </button>
    </section>
  )
}

export function ShowcasePage() {
  const { t, lang } = useLocale()
  const ts = t.showcase
  const [allTales, setAllTales] = useState<ShowcaseStoryDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let cancelled = false
    showcase.list()
      .then(list => { if (!cancelled) setAllTales(list) })
      .catch(() => { if (!cancelled) setError(true) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  // Show tales in the active site language; fall back to all if none match
  // (avoids an empty gallery when the curated set is single-language).
  const inLang = allTales.filter(tale => tale.language === lang)
  const tales = inLang.length > 0 ? inLang : allTales

  const cover = (tale: ShowcaseStoryDto) => tale.panels?.[0]?.imageUrl ?? null
  const spot = tales[0]
  const rest = tales.slice(1)

  return (
    <div className="wrap" style={{ paddingTop: 44 }}>
      <div className={styles.head}>
        <div className="eyebrow">{ts.label}</div>
        <h1 className={styles.h1}>{ts.heading}</h1>
        <p className={styles.sub}>{ts.intro}</p>
      </div>

      {loading && <p className={styles.msg}>{ts.loading}</p>}
      {!loading && error && <p className={styles.msg}>{ts.error}</p>}
      {!loading && !error && tales.length === 0 && <p className={styles.msg}>{ts.empty}</p>}

      {!loading && !error && spot && (
        <Link className={styles.spot} to={`/showcase/${spot.id}`}>
          {cover(spot) && <img className={styles.spotImg} src={cover(spot)!} alt="" aria-hidden="true" />}
          <div className={styles.spotShade} />
          <div className={styles.spotBody}>
            <div className={styles.spotEyebrow}>{ts.week}</div>
            <div className={styles.spotTitle}>{spot.title}</div>
            {spot.theme && <div className={styles.spotMeta}>{spot.theme}</div>}
            <span className={`btn btn-primary ${styles.spotBtn}`}>{ts.read}</span>
          </div>
        </Link>
      )}

      {!loading && !error && rest.length > 0 && (
        <div className="grid-stories" style={{ marginTop: 40 }}>
          {rest.map(tale => (
            <TapedCard
              key={tale.id}
              rotationKey={tale.id}
              cover={cover(tale)}
              coverAlt={tale.title}
              coverHeight={186}
              title={tale.title}
              meta={tale.theme ?? undefined}
              to={`/showcase/${tale.id}`}
            />
          ))}
        </div>
      )}

      <ShowcaseCta />
    </div>
  )
}
