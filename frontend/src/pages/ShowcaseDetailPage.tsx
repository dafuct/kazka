import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ComicsReader } from '../components/comics/ComicsReader'
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
      <div className={styles.page}>
        <div className={styles.inner}>
          <div className={styles.topBar}>
            <Link to="/" className={styles.back}>{ts.backToGallery}</Link>
          </div>
          <p className={styles.state}>{ts.notFound}</p>
          <div className={styles.ctaWrap}>
            <ShowcaseCta />
          </div>
        </div>
      </div>
    )
  }

  const paragraphs = tale.content
    .split(/\n\s*\n+/)
    .map(p => p.trim())
    .filter(Boolean)

  return (
    <div className={styles.page}>
      <div className={styles.inner}>
        <div className={styles.topBar}>
          <Link to="/" className={styles.back}>{ts.backToGallery}</Link>
          <span className={styles.readonlyBadge}>{ts.readonlyNote}</span>
        </div>

        <h1 className={styles.title}>{tale.title}</h1>

        <div className={styles.meta}>
          <span className={styles.tag}>{tale.ageGroup}</span>
          <span className={styles.tag}>{tale.length}</span>
          <span className={styles.tag}>{tale.language.toUpperCase()}</span>
        </div>

        <div className={styles.comicsBlock}>
          {/* Read-only: no onRetry handler — visitors cannot regenerate anything. */}
          <ComicsReader story={tale} />
        </div>

        <div className={styles.content}>
          {paragraphs.map((para, i) => (
            <p key={i}>{para}</p>
          ))}
        </div>

        <div className={styles.ctaWrap}>
          <ShowcaseCta />
        </div>
      </div>
    </div>
  )
}
