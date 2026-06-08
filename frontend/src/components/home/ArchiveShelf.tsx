import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../lib/apiClient'
import type { Story } from '../../lib/types'
import { useReveal } from '../../lib/useReveal'
import { useLocale } from '../../lib/LocaleContext'
import { SectionParticles } from './SectionParticles'
import styles from './ArchiveShelf.module.css'

const SPINE_COLORS = [
  'linear-gradient(160deg,#D97706,#B45309)',
  'linear-gradient(160deg,#166534,#15803D)',
  'linear-gradient(160deg,#7E2A33,#9C2F4A)',
  'linear-gradient(160deg,#211C18,#2E6E82)',
  'linear-gradient(160deg,#C2410C,#991B1B)',
  'linear-gradient(160deg,#92400E,#78350F)',
  'linear-gradient(160deg,#2F6B43,#2E6E82)',
  'linear-gradient(160deg,#166534,#064E3B)',
]
const TILTS = ['-2deg', '1deg', '-1deg', '2deg', '-1.5deg', '0.5deg', '-2.5deg', '1.5deg']

interface SpineProps {
  title: string
  color: string
  tilt: string
  delay: number
  href?: string
}

function Spine({ title, color, tilt, delay, href }: SpineProps) {
  const ref = useRef<HTMLDivElement>(null)
  const [dropped, setDropped] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setTimeout(() => setDropped(true), delay)
          obs.unobserve(el)
        }
      },
      { threshold: 0.2 }
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [delay])

  const inner = (
    <div
      ref={ref}
      className={`${styles.spine} ${dropped ? styles.dropped : ''}`}
      style={{ background: color, '--tilt': tilt } as React.CSSProperties}
    >
      <span className={styles.spineTitle}>{title}</span>
    </div>
  )

  return href ? <Link to={href} className={styles.spineLink}>{inner}</Link> : inner
}

export function ArchiveShelf() {
  const { t } = useLocale()
  const { ref: headRef, visible: headVisible } = useReveal()
  const [stories, setStories] = useState<Story[]>([])

  useEffect(() => {
    api.listStories(0, 8).then(res => setStories(res.items)).catch(() => null)
  }, [])

  const featured = stories[0]
  const spines = stories.slice(1)
  const placeholders = t.archiveShelf.placeholders

  return (
    <section className={styles.section} id="archive">
      <SectionParticles />
      <div className={styles.inner}>
        <div ref={headRef} className={`reveal ${headVisible ? 'visible' : ''}`}>
          <div className={styles.label}>{t.archiveShelf.label}</div>
          <div className={styles.title}>{t.archiveShelf.title}</div>
        </div>

        <div className={`${styles.shelf} reveal ${headVisible ? 'visible' : ''}`}>
          {featured ? (
            <Link to={`/stories/${featured.id}`} className={styles.featuredLink}>
              <div className={styles.featured}>
                <div className={styles.illustBook} />
                <span className={styles.featuredLabel}>{featured.title || t.archiveShelf.latestStory}</span>
              </div>
            </Link>
          ) : (
            <div className={styles.featured}>
              <div className={styles.illustBook} />
              <span className={styles.featuredLabel}>{t.archiveShelf.latestStory}</span>
            </div>
          )}

          {(spines.length > 0 ? spines : placeholders.map(title => ({ id: '', title } as Story))).map((s, i) => (
            <Spine
              key={s.id || s.title}
              title={s.title || placeholders[i]}
              color={SPINE_COLORS[i % SPINE_COLORS.length]}
              tilt={TILTS[i % TILTS.length]}
              delay={i * 100}
              href={s.id ? `/stories/${s.id}` : undefined}
            />
          ))}
        </div>
      </div>
    </section>
  )
}
