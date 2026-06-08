import { useEffect, useState } from 'react'
import { OrnamentBand } from '../../components/stitch/OrnamentBand'
import ReactMarkdown from 'react-markdown'
import { useLocale } from '../../lib/LocaleContext'
import styles from './LegalPage.module.css'

type Slug = 'terms' | 'privacy' | 'support'

// Vite ?raw imports the file content as a string at build time — no runtime fetch.
const sources = {
  'terms-uk':   () => import('./content/terms.uk.md?raw'),
  'terms-en':   () => import('./content/terms.en.md?raw'),
  'privacy-uk': () => import('./content/privacy.uk.md?raw'),
  'privacy-en': () => import('./content/privacy.en.md?raw'),
  'support-uk': () => import('./content/support.uk.md?raw'),
  'support-en': () => import('./content/support.en.md?raw'),
} as const

export function LegalPage({ slug }: { slug: Slug }) {
  const { lang, t } = useLocale()
  const [text, setText] = useState<string>('')

  useEffect(() => {
    // Guard against a stale loader resolving after a newer slug/lang has been requested.
    let cancelled = false
    const key = `${slug}-${lang}` as keyof typeof sources
    const loader = sources[key] ?? sources[`${slug}-uk` as keyof typeof sources]
    loader().then(m => { if (!cancelled) setText(m.default) })
    return () => { cancelled = true }
  }, [slug, lang])

  const title = t.legal[slug]

  useEffect(() => {
    document.title = `${title} · ${t.brand}`
  }, [title, t.brand])

  return (
    <main className={`${styles.page} kz-page`}><OrnamentBand framed={false} stitch={5} cols={120} className="kz-orn-top" />
      <article className={styles.prose}>
        <ReactMarkdown>{text}</ReactMarkdown>
      </article>
    </main>
  )
}
