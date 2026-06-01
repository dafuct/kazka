import type { components } from '@kazka/shared'
import styles from './ComicsReader.module.css'

type Story = components['schemas']['StoryDto']

export interface ComicsReaderProps {
  story: Story
  onRetry?: () => void
}

export function ComicsReader({ story, onRetry }: ComicsReaderProps) {
  if (story.illustrationStatus === 'FAILED') {
    return (
      <div className={styles.failed}>
        <p>Не вдалося згенерувати комікс.</p>
        {onRetry ? (
          <button className={styles.retryButton} onClick={onRetry} type="button">
            Спробувати ще раз
          </button>
        ) : null}
      </div>
    )
  }

  const page = story.panels[0]
  if (!page) {
    return <div className={styles.skeleton} aria-label="Малюємо комікс…" />
  }
  return (
    <div className={styles.pageWrap}>
      <img className={styles.page} src={page.imageUrl} alt={story.title} />
    </div>
  )
}
