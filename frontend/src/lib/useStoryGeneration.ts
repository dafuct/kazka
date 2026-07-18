import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { streamStory } from './sseClient'
import { useActiveStory } from './ActiveStoryContext'
import { useAuth } from './AuthContext'
import { useLocale } from './LocaleContext'
import type { GenerationRequest, ModerationErrorCode } from './types'

const MODERATION_CODES: readonly ModerationErrorCode[] = ['BLOCKED_INPUT', 'JUDGE_UNAVAILABLE']

/** Story-generation SSE flow (extracted from the retired StoryModal).
    SSE is triggered IMPERATIVELY from generate() (not from a useEffect) so
    React StrictMode's double-effect-invocation can't kill the stream. The
    AbortController is parked in a ref for real-unmount cleanup. */
export function useStoryGeneration() {
  const navigate = useNavigate()
  const { t } = useLocale()
  const { refresh } = useAuth()
  const { setActiveStoryId } = useActiveStory()
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const activeCtrlRef = useRef<AbortController | null>(null)

  // Real-unmount cleanup only (empty deps → survives the StrictMode dance).
  useEffect(() => {
    return () => {
      activeCtrlRef.current?.abort()
      activeCtrlRef.current = null
    }
  }, [])

  const generate = useCallback((req: GenerationRequest) => {
    activeCtrlRef.current?.abort()
    const ctrl = new AbortController()
    activeCtrlRef.current = ctrl

    setError(null)
    setGenerating(true)

    streamStory(
      req,
      {
        onMeta: ({ id }) => {
          if (ctrl.signal.aborted) return
          // Story row exists — route the user to the story page immediately;
          // the SSE keeps streaming via the controller stored in activeCtrlRef.
          setActiveStoryId(id)
          navigate(`/stories/${id}`)
        },
        onToken: () => {},
        onDone: () => {
          if (ctrl.signal.aborted) return
          // The SERVER kicks off the comic build automatically — do not
          // trigger it client-side (it would race the server's build).
          activeCtrlRef.current = null
        },
        onError: ({ code, category, message }) => {
          if (ctrl.signal.aborted) return
          if (code === 'MONTHLY_LIMIT') {
            setError(t.story.monthlyLimit)
          } else if (code && (MODERATION_CODES as readonly string[]).includes(code)) {
            const perCategory = code === 'BLOCKED_INPUT' && category
              ? t.moderation.byCategory?.[category]
              : undefined
            setError(perCategory ?? t.moderation[code as ModerationErrorCode])
            refresh()
          } else {
            setError(message ?? null)
          }
          setGenerating(false)
          activeCtrlRef.current = null
        },
      },
      ctrl.signal,
    ).catch(err => {
      if (ctrl.signal.aborted || err?.name === 'AbortError') return
      setError(String(err))
      setGenerating(false)
      activeCtrlRef.current = null
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [setActiveStoryId, navigate, refresh, t])

  return { generate, generating, error, clearError: useCallback(() => setError(null), []) }
}
