import { useEffect, useRef, useState } from 'react'
import { streamStory } from '../../lib/sseClient'
import type { GenerationRequest } from '../../lib/types'
import styles from './StoryStream.module.css'

interface StoryStreamProps {
  request: GenerationRequest
  onDone: (id: string, title: string) => void
  onError: (message: string) => void
}

export function StoryStream({ request, onDone, onError }: StoryStreamProps) {
  const [tokens, setTokens] = useState<string[]>([])
  const abortRef = useRef<AbortController | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctrl = new AbortController()
    abortRef.current = ctrl
    setTokens([])

    streamStory(
      request,
      {
        onToken: ({ text }) => setTokens(prev => [...prev, text]),
        onDone: ({ id, title }) => onDone(id, title),
        onError: ({ message }) => onError(message),
      },
      ctrl.signal
    ).catch(err => {
      if (err?.name !== 'AbortError') onError(String(err))
    })

    return () => ctrl.abort()
  }, [])

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight
    }
  }, [tokens])

  const text = tokens.join('')

  return (
    <div ref={containerRef} className={styles.container}>
      <p className={styles.text}>
        {text}
        {tokens.length > 0 && <span className={styles.cursor} />}
      </p>
    </div>
  )
}
