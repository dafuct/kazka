import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { TapedCard } from '../components/taped/TapedCard'
import { useLocale } from '../lib/LocaleContext'
import { useAuth } from '../lib/AuthContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { api, showcase } from '../lib/apiClient'
import type { ShowcaseStoryDto } from '../lib/apiClient'
import type { Story } from '../lib/types'
import styles from './HomePage.module.css'

export function HomePage() {
  const { t } = useLocale()
  const { user } = useAuth()
  const { openAuth } = useAuthModal()
  const navigate = useNavigate()
  const [idea, setIdea] = useState('')
  const [samples, setSamples] = useState<ShowcaseStoryDto[]>([])
  const [mine, setMine] = useState<Story[]>([])

  useEffect(() => {
    let cancelled = false
    showcase.list()
      .then(list => { if (!cancelled) setSamples(list) })
      .catch(() => null)
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!user) return
    let cancelled = false
    api.listStories(0, 4)
      .then(res => { if (!cancelled) setMine(res.items) })
      .catch(() => null)
    return () => { cancelled = true }
  }, [user])

  const goCreate = (prefill?: string) => {
    if (!user) { openAuth('signUp'); return }
    const q = prefill?.trim() ? `?idea=${encodeURIComponent(prefill.trim())}` : ''
    navigate(`/create${q}`)
  }

  const onIdeaSubmit = (e: FormEvent) => {
    e.preventDefault()
    goCreate(idea)
  }

  const cover = (s: { panels?: { imageUrl?: string }[] }) => s.panels?.[0]?.imageUrl ?? null

  const featured = user && mine.length > 0
    ? { heading: t.home.mineH2, seeAllTo: '/stories', items: mine.map(s => ({ id: s.id, title: s.title, cover: cover(s), to: `/stories/${s.id}` })) }
    : { heading: t.home.featH2, seeAllTo: '/showcase', items: samples.slice(0, 4).map(s => ({ id: s.id, title: s.title, cover: cover(s), to: `/showcase/${s.id}` })) }

  return (
    <div>
      {/* HERO */}
      <div className="wrap">
        <section className={styles.hero}>
          <div className="fadein">
            <div className="eyebrow" style={{ marginBottom: 14 }}>{t.home.eyebrow}</div>
            <h1 className={styles.h1}>
              {t.home.h1a}
              <span className={styles.hl}>{t.home.h1b}</span>
              {t.home.h1c}
            </h1>
            <p className={styles.sub}>{t.home.sub}</p>
            <form className={styles.ideaBox} onSubmit={onIdeaSubmit}>
              <input
                value={idea}
                onChange={e => setIdea(e.target.value)}
                placeholder={t.home.ideaPh}
              />
              <button type="submit" className="btn btn-primary">{t.home.createBtn}</button>
            </form>
          </div>
          <div className={`${styles.heroArt} fadein`}>
            <TapedCard
              rotationKey="hero-a"
              cover={samples[0] ? cover(samples[0]) : null}
              coverAlt={samples[0]?.title ?? ''}
              coverHeight={300}
              className={styles.heroCardA}
            />
            <TapedCard
              rotationKey="hero-b"
              cover={samples[1] ? cover(samples[1]) : null}
              coverAlt={samples[1]?.title ?? ''}
              coverHeight={224}
              className={styles.heroCardB}
            />
          </div>
        </section>
      </div>

      {/* FEATURED */}
      <div className="wrap" style={{ marginTop: 40 }}>
        <section>
          <div className="sec-head">
            <div>
              <div className="eyebrow">{t.home.featEyebrow}</div>
              <h2>{featured.heading}</h2>
            </div>
            <span className="link-more" onClick={() => navigate(featured.seeAllTo)}>{t.home.seeAll}</span>
          </div>
          <div className="grid-stories">
            {featured.items.map(item => (
              <TapedCard
                key={item.id}
                rotationKey={item.id}
                cover={item.cover}
                coverAlt={item.title}
                coverHeight={186}
                title={item.title}
                to={item.to}
              />
            ))}
          </div>
        </section>
      </div>

      {/* HOW IT WORKS */}
      <div className="wrap" style={{ marginTop: 80 }}>
        <section>
          <div className={styles.hiwHead}>
            <div className="eyebrow">{t.home.hiwEyebrow}</div>
            <h2 className={styles.hiwH2}>{t.home.hiwH2}</h2>
          </div>
          <div className={styles.steps}>
            {[
              ['1', t.home.s1t, t.home.s1d],
              ['2', t.home.s2t, t.home.s2d],
              ['3', t.home.s3t, t.home.s3d],
            ].map(([n, title, desc]) => (
              <div className={styles.step} key={n}>
                <div className={styles.stepN}>{n}</div>
                <h3>{title}</h3>
                <p>{desc}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* FEATURES */}
      <div className="wrap" style={{ marginTop: 80 }}>
        <section>
          <div className="sec-head">
            <div>
              <div className="eyebrow">{t.home.capEyebrow}</div>
              <h2>{t.home.capH2}</h2>
            </div>
          </div>
          <div className={styles.featGrid}>
            {[
              [t.home.f1t, t.home.f1d],
              [t.home.f2t, t.home.f2d],
            ].map(([title, desc]) => (
              <div className={styles.feat} key={title}>
                <h3>{title}</h3>
                <p>{desc}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* CTA BAND */}
      <div className="wrap" style={{ marginTop: 80 }}>
        <section className="band">
          <h2>{t.home.bandH2}</h2>
          <p>{t.home.bandP}</p>
          <button type="button" className="btn btn-primary btn-lg" onClick={() => goCreate()}>
            {t.home.bandBtn}
          </button>
        </section>
      </div>
    </div>
  )
}
