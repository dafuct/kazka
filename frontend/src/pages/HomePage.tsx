import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { StoryForm } from '../components/form/StoryForm'
import { StoryStream } from '../components/story/StoryStream'
import { HowItWorks } from '../components/home/HowItWorks'
import { Features } from '../components/home/Features'
import { NightCta } from '../components/home/NightCta'
import { useLocale } from '../lib/LocaleContext'
import type { GenerationRequest } from '../lib/types'
import styles from './HomePage.module.css'

type Phase = 'form' | 'streaming' | 'done'

export function HomePage() {
  const { t } = useLocale()
  const navigate = useNavigate()
  const [phase, setPhase] = useState<Phase>('form')
  const [request, setRequest] = useState<GenerationRequest | null>(null)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = useCallback((req: GenerationRequest) => {
    setRequest(req)
    setError(null)
    setPhase('streaming')
  }, [])

  const handleDone = useCallback((id: string) => {
    setPhase('done')
    navigate(`/stories/${id}`)
  }, [navigate])

  const handleError = useCallback((message: string) => {
    setError(message)
    setPhase('form')
  }, [])

  return (
    <div>
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <h1 className={styles.heroTitle}>{t.home.hero}</h1>
          <p className={styles.heroTagline}>{t.home.tagline}</p>
        </div>
      </section>

      <section className={styles.formSection} id="generate">
        <div className={styles.formWrapper}>
          {error && <div className={styles.error}>{error}</div>}
          {phase === 'form' && (
            <StoryForm onSubmit={handleSubmit} loading={false} />
          )}
          {phase === 'streaming' && request && (
            <div className={styles.streaming}>
              <h2 className={styles.streamingTitle}>{t.form.generating}</h2>
              <StoryStream
                request={request}
                onDone={handleDone}
                onError={handleError}
              />
            </div>
          )}
        </div>
      </section>

      <HowItWorks />
      <Features />
      <NightCta />
    </div>
  )
}
