import { useEffect, useRef, Fragment } from 'react'
import { ScMotif, SCM, THREAD } from '../components/stitch/StitchCanvas'
import { IllustrationCarousel } from '../components/illustrations/IllustrationCarousel'
import { HowItWorks } from '../components/home/HowItWorks'
import { Features } from '../components/home/Features'
import { StoryPreview } from '../components/home/StoryPreview'
import { NightCta } from '../components/home/NightCta'
import { AvatarInitials } from '../components/children/AvatarInitials'
import { HolidayChip } from '../components/holidays/HolidayChip'
import { useLocale } from '../lib/LocaleContext'
import { useStoryModal } from '../lib/StoryModalContext'
import { useAuth } from '../lib/AuthContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { useChildren } from '../lib/ChildrenContext'
import { handleRipple } from '../lib/ripple'
import styles from './HomePage.module.css'

function ParticleField() {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const field = ref.current
    if (!field) return
    const types = ['star', 'dot', 'circle', 'dash'] as const
    for (let i = 0; i < 28; i++) {
      const el = document.createElement('div')
      const type = types[i % 4]
      el.className = `${styles.particle} ${styles['particle_' + type]}`
      const x = Math.random() * 100
      const y = Math.random() * 100
      const dur = 8 + Math.random() * 12
      const delay = Math.random() * 10
      const opacity = 0.3 + Math.random() * 0.5
      el.style.cssText = `left:${x}%;top:${y}%;--p-opacity:${opacity};`
      if (type === 'star') {
        el.innerHTML = '<svg width="12" height="12" viewBox="0 0 16 16"><path d="M8 0L9.5 6.5L16 8L9.5 9.5L8 16L6.5 9.5L0 8L6.5 6.5Z" fill="currentColor"/></svg>'
        el.style.animation = `orbitalDrift ${dur}s ease-in-out ${delay}s infinite`
      } else if (type === 'dot') {
        const s = 2 + Math.random() * 3
        el.style.width = s + 'px'
        el.style.height = s + 'px'
        el.style.animation = `floatUp ${dur}s linear ${delay}s infinite`
      } else if (type === 'circle') {
        const s = 6 + Math.random() * 10
        el.style.width = s + 'px'
        el.style.height = s + 'px'
        el.style.animation = `expandFade ${dur * 0.6}s ease-out ${delay}s infinite`
      } else {
        el.style.width = (8 + Math.random() * 12) + 'px'
        el.style.height = '2px'
        el.style.animation = `floatUp ${dur}s linear ${delay}s infinite`
      }
      field.appendChild(el)
    }
    return () => { field.innerHTML = '' }
  }, [])
  return <div ref={ref} className={styles.particleField} aria-hidden="true" />
}


export function HomePage() {
  const { t } = useLocale()
  const { openModal } = useStoryModal()
  const { user } = useAuth()
  const { openAuth } = useAuthModal()
  const { active } = useChildren()
  const tc = (t as any).children ?? {}
  const tryClick = (e: React.MouseEvent<HTMLElement>) => {
    e.preventDefault()
    if (!user) openAuth('signIn'); else openModal()
    handleRipple(e)
  }
  const heroImgRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let ticking = false
    const onScroll = () => {
      if (!ticking) {
        requestAnimationFrame(() => {
          if (heroImgRef.current) {
            heroImgRef.current.style.transform = `translateY(${window.scrollY * 0.12}px)`
          }
          ticking = false
        })
        ticking = true
      }
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div className={styles.home}>
      {/* ── HERO ── */}
      <section className={styles.hero}>
        <ParticleField />
        <div className={styles.heroInner}>
          <div className={styles.heroMotif} aria-hidden="true">
            <ScMotif rule={SCM.medallion} n={21} stitch={7} palette={THREAD} ground="var(--color-surface)" />
          </div>
          <div className={styles.heroText}>
            <div className={styles.heroLabel}>{t.home.label}</div>
            <h1 className={styles.heroHeadline}>
              {t.home.headline.split(' ').map((word, i) => (
                <Fragment key={i}>
                  <span
                    className={styles.heroWord}
                    style={{ animationDelay: `${i * 0.1}s` }}
                  >{word}</span>{' '}
                </Fragment>
              ))}
            </h1>
            <p className={styles.heroSub}>{t.home.sub}</p>
            {user && active && (
              <HolidayChip onApply={(theme) => localStorage.setItem('kazka.suggestedTheme', theme)} />
            )}
            <div className={styles.heroButtons}>
              <a
                href="#"
                className={styles.btnPrimary}
                onClick={tryClick}
              >
                {t.home.cta} →
              </a>
              <a href="#preview" className={styles.btnSecondary}>
                {t.home.previewBtn}
              </a>
            </div>
            <div className={styles.heroProof}>{t.home.proof}</div>
            {user && active && (
              <div className={styles.activeChildPill}>
                <AvatarInitials name={active.name} seed={active.avatarSeed} size={20} />
                <span>{tc.generatingFor ? tc.generatingFor(active.name) : `For ${active.name}`}</span>
              </div>
            )}
          </div>

          <div ref={heroImgRef} className={styles.heroImageWrap}>
            <div className={styles.heroImage}>
              <IllustrationCarousel section="hero" />
            </div>
          </div>
        </div>
      </section>

      <HowItWorks />
      <Features />
      <StoryPreview />
      <NightCta />
    </div>
  )
}
