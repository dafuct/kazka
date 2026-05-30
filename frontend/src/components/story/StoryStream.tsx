import { useEffect, useRef, useState } from 'react'
import { streamStory } from '../../lib/sseClient'
import type { GenerationRequest, ModerationErrorCode, ModerationCategory } from '../../lib/types'
import { RefusalCard } from './RefusalCard'
import { useAuth } from '../../lib/AuthContext'
import styles from './StoryStream.module.css'

interface StoryStreamProps {
  request: GenerationRequest
  onDone: (id: string, title: string) => void
  onError: (message: string) => void
  onTryAnother: () => void
}

const MODERATION_CODES: readonly ModerationErrorCode[] = ['BLOCKED_INPUT', 'JUDGE_UNAVAILABLE']

export function StoryStream({ request, onDone, onError, onTryAnother }: StoryStreamProps) {
  const [tokens, setTokens] = useState<string[]>([])
  const [refusal, setRefusal] = useState<{ code: ModerationErrorCode; category?: ModerationCategory } | null>(null)
  const abortRef = useRef<AbortController | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const { refresh } = useAuth()

  useEffect(() => {
    const ctrl = new AbortController()
    abortRef.current = ctrl
    setTokens([])
    setRefusal(null)

    streamStory(
      request,
      {
        onToken: ({ text }) => setTokens(prev => [...prev, text]),
        onDone: ({ id, title }) => onDone(id, title),
        onError: ({ code, category, message }) => {
          if (code && (MODERATION_CODES as readonly string[]).includes(code)) {
            setRefusal({ code: code as ModerationErrorCode, category })
            // Suspension may have just kicked in; refresh AuthContext
            refresh()
            return
          }
          onError(message ?? code ?? 'ERROR')
        },
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

  if (refusal) return <RefusalCard code={refusal.code} category={refusal.category} onTryAnother={onTryAnother} />

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
