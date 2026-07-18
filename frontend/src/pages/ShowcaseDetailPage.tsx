import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ComicsReader } from '../components/comics/ComicsReader'
import { TapedCard } from '../components/taped/TapedCard'
import { StoryReader } from '../components/reader/StoryReader'
import { ShowcaseCta } from './ShowcasePage'
import { useLocale } from '../lib/LocaleContext'
import { showcase } from '../lib/apiClient'
import type { ShowcaseStoryDto } from '../lib/apiClient'
import styles from './ShowcaseDetailPage.module.css'

export function ShowcaseDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useLocale()
  const ts = t.showcase

  const [tale, setTale] = useState<ShowcaseStoryDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [readerOpen, setReaderOpen] = useState(false)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setNotFound(false)
    showcase.get(id)
      .then(s => { if (!cancelled) setTale(s) })
      .catch(() => { if (!cancelled) setNotFound(true) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [id])

  if (loading) return <div className={styles.state}>{ts.loading}</div>

  if (notFound || !tale) {
    return (
      <div className="wrap">
        <div className={styles.topBar}>
          <Link to="/showcase" className="link-more">{ts.backToGallery}</Link>
        </div>
        <p className={styles.state}>{ts.notFound}</p>
        <ShowcaseCta />
      </div>
    )
  }

  const firstParagraph = tale.content.split(/\n\s*\n+/).map(p => p.trim()).filter(Boolean)[0] ?? ''
  const cover = tale.panels?.[0]?.imageUrl ?? null

  return (
    <div className="wrap">
      <div className={styles.topBar}>
        <Link to="/showcase" className="link-more">{ts.backToGallery}</Link>
        <span className="badge">{ts.readonlyNote}</span>
      </div>

      <section className={styles.top}>
        <div className="fadein">
          <TapedCard rotationKey={tale.id} className={styles.coverCard}>
            {/* Read-only: no onRetry — visitors cannot regenerate anything. */}
            <ComicsReader story={tale} />
          </TapedCard>
        </div>

        <div className="fadein">
          {tale.theme && <div className="eyebrow">{tale.theme}</div>}
          <h1 className={styles.title}>{tale.title}</h1>

          <div className={styles.meta}>
            <span className="badge">{t.form.ageGroups[tale.ageGroup as '3-5' | '6-8' | '9-12']}</span>
            <span className="badge">{t.form.lengths[tale.length as 'short' | 'medium' | 'long']}</span>
            <span className="badge">{t.form.languages[tale.language as 'uk' | 'en'] ?? tale.language.toUpperCase()}</span>
            {tale.panels.length > 0 && <span className="badge">{t.detail.illustrated}</span>}
          </div>

          <div className={styles.cta}>
            <button type="button" className="btn btn-primary btn-lg" onClick={() => setReaderOpen(true)}>
              {t.detail.read}
            </button>
          </div>

          {firstParagraph && (
            <>
              <div className="field-label">{t.detail.about}</div>
              <p className={`serif ${styles.blurb}`}>{firstParagraph}</p>
            </>
          )}
        </div>
      </section>

      <ShowcaseCta />

      {readerOpen && (
        <StoryReader
          title={tale.title}
          text={tale.content}
          cover={cover}
          onClose={() => setReaderOpen(false)}
        />
      )}
    </div>
  )
}
