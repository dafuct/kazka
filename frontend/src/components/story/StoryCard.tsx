import { Link } from 'react-router-dom'
import type { Story } from '../../lib/types'
import { IllustrationFrame } from './IllustrationFrame'
import styles from './StoryCard.module.css'

interface StoryCardProps {
  story: Story
  onDelete: (id: string) => void
  badge?: React.ReactNode
}

export function StoryCard({ story, onDelete, badge }: StoryCardProps) {
  const preview = story.content.slice(0, 120).replace(/\n/g, ' ')

  return (
    <article className={styles.card}>
      <Link to={`/stories/${story.id}`} className={styles.imageLink}>
        <IllustrationFrame
          pathLight={story.illustrationPathLight}
          pathDark={story.illustrationPathDark}
          status={story.illustrationStatus}
        />
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
