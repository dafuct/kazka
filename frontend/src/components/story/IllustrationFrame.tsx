import { useState } from 'react'
import { PlaceholderSvg } from './PlaceholderSvg'
import { useTheme } from '../../lib/ThemeContext'
import type { IllustrationStatus } from '../../lib/types'
import styles from './IllustrationFrame.module.css'

interface IllustrationFrameProps {
  pathLight: string | null
  pathDark: string | null
  status: IllustrationStatus
}

export function IllustrationFrame({ pathLight, pathDark, status }: IllustrationFrameProps) {
  const [imgError, setImgError] = useState(false)
  const { theme } = useTheme()
  const path = theme === 'dark' ? pathDark ?? pathLight : pathLight ?? pathDark

  if (status === 'READY' && path && !imgError) {
    return (
      <div className={styles.frame}>
        <img
          src={path}
          alt="Ілюстрація казки"
          className={styles.img}
          onError={() => setImgError(true)}
        />
      </div>
    )
  }

  if (status === 'PENDING') {
    return (
      <div className={styles.frame}>
        <div className={styles.skeleton}>
          <PlaceholderSvg />
          <div className={styles.shimmer} />
        </div>
      </div>
    )
  }

  return (
    <div className={styles.frame}>
      <PlaceholderSvg />
    </div>
  )
}
