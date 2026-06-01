import { Link } from 'react-router-dom'
import type { Story } from '../../lib/types'
import { InProgressCard } from '../comics/InProgressCard'
import styles from './StoryCard.module.css'

interface StoryCardProps {
  story: Story
  onDelete: (id: string) => void
  badge?: React.ReactNode
}

export function StoryCard({ story, onDelete, badge }: StoryCardProps) {
  const preview = story.content.slice(0, 120).replace(/\n/g, ' ')
  const cover = story.panels[0]?.imageUrl ?? null
  const failed = story.illustrationStatus === 'FAILED'

  return (
    <article className={styles.card}>
      <Link to={`/stories/${story.id}`} className={styles.imageLink}>
        {failed ? (
          <div className={styles.errorBadge} role="img" aria-label="Помилка ілюстрації">
            ⚠ Ілюстрації не вдалися
          </div>
        ) : cover ? (
          <img src={cover} alt={story.title} className={styles.cover} />
        ) : (
          <InProgressCard title={story.title} />
        )}
      </Link>
      <div className={styles.body}>
        <div className={styles.titleRow}>
          <Link to={`/stories/${story.id}`} className={styles.titleLink}>
            <h3 className={styles.title}>{story.title}</h3>
          </Link>
          {badge}
        </div>
        <p className={styles.preview}>{preview}…</p>
        <div className={styles.meta}>
          <span className={styles.tag}>{story.ageGroup}</span>
          <span className={styles.tag}>{story.length}</span>
          <button
            className={styles.deleteBtn}
            onClick={() => onDelete(story.id)}
            aria-label="Видалити казку"
          >
            ✕
          </button>
        </div>
      </div>
    </article>
  )
}
