import type { components } from '@kazka/shared'
import { useLocale } from '../../lib/LocaleContext'
import styles from './ComicsReader.module.css'

type Story = components['schemas']['StoryDto']

export interface ComicsReaderProps {
  story: Story
  onRetry?: () => void
}

export function ComicsReader({ story, onRetry }: ComicsReaderProps) {
  const { t } = useLocale()
  if (story.illustrationStatus === 'FAILED') {
    return (
      <div className={styles.failed}>
        <p>{t.comics.failed}</p>
        {onRetry ? (
          <button className={styles.retryButton} onClick={onRetry} type="button">
            {t.comics.retry}
          </button>
        ) : null}
      </div>
    )
  }

  const page = story.panels[0]
  if (!page) {
    return <div className={styles.skeleton} aria-label={t.comics.drawing} />
  }
  return (
    <div className={styles.pageWrap}>
      <img className={styles.page} src={page.imageUrl} alt={story.title} />
    </div>
  )
}
