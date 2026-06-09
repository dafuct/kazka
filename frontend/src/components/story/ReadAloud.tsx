import { useEffect, useRef, useState } from 'react'
import styles from './ReadAloud.module.css'
import { narration } from '../../lib/apiClient'

interface ReadAloudProps {
  /** Story id — used to fetch/cache server-side neural narration. */
  storyId: string
  /** Plain text of the tale — used only for the Web Speech fallback. */
  text: string
  /** BCP-47 language of the story text, e.g. "uk" or "en" */
  lang?: string
  label: string
  stopLabel: string
  preparingLabel: string
  narrator?: string
}

type Phase = 'idle' | 'preparing' | 'playing'

/**
 * Read-aloud control. Prefers high-quality server-side neural narration (Gemini TTS, cached and
 * served as an <audio> clip). Falls back to the browser Web Speech API when the narration request
 * fails or the audio can't play. Generation is warm-started when the tale opens (see effect below),
 * so playback is usually instant on tap; if it isn't ready yet, a brief "preparing" state is shown.
 */
export function ReadAloud({ storyId, text, lang = 'uk', label, stopLabel, preparingLabel, narrator }: ReadAloudProps) {
  const [phase, setPhase] = useState<Phase>('idle')
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const pollRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const cancelledRef = useRef(false)

  // Stop and reset when the tale changes or the component unmounts.
  useEffect(() => {
    cancelledRef.current = false
    return () => {
      cancelledRef.current = true
      if (pollRef.current) clearTimeout(pollRef.current)
      audioRef.current?.pause()
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
    }
  }, [storyId])

  // Warm-start: kick off server-side narration generation as soon as the tale opens, so the clip
  // is (likely) ready by the time the reader taps — turning a ~45s synthesis wait into near-instant.
  // Idempotent server-side (atomic claim), so this never double-generates: one synthesis per tale,
  // cached afterwards. We do NOT auto-play — playback still requires a tap (user gesture).
  useEffect(() => {
    narration.request(storyId).catch(() => {
      /* ignore here — the button tap re-requests and owns error handling / Web Speech fallback */
    })
  }, [storyId])

  const fallback = () => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      setPhase('idle')
      return
    }
    const synth = window.speechSynthesis
    const u = new SpeechSynthesisUtterance(text)
    const base = lang.slice(0, 2).toLowerCase()
    u.lang = base === 'uk' ? 'uk-UA' : base
    const match = synth.getVoices().find((v) => v.lang?.toLowerCase().startsWith(base))
    if (match) u.voice = match
    u.rate = 0.96
    u.onend = () => setPhase('idle')
    u.onerror = () => setPhase('idle')
    synth.cancel()
    synth.speak(u)
    setPhase('playing')
  }

  const play = (url: string) => {
    if (cancelledRef.current) return
    if (!audioRef.current) {
      audioRef.current = new Audio()
      audioRef.current.onended = () => setPhase('idle')
      audioRef.current.onerror = () => fallback()
    }
    audioRef.current.src = url
    audioRef.current.play().then(() => setPhase('playing')).catch(() => fallback())
  }

  const poll = async () => {
    try {
      const res = await narration.get(storyId)
      if (cancelledRef.current) return
      if (res.status === 'READY' && res.url) {
        play(res.url)
      } else if (res.status === 'FAILED') {
        fallback()
      } else {
        pollRef.current = setTimeout(poll, 2000)
      }
    } catch {
      fallback()
    }
  }

  const start = async () => {
    setPhase('preparing')
    try {
      const res = await narration.request(storyId)
      if (cancelledRef.current) return
      if (res.status === 'READY' && res.url) {
        play(res.url)
      } else if (res.status === 'FAILED') {
        fallback()
      } else {
        pollRef.current = setTimeout(poll, 2000)
      }
    } catch {
      fallback()
    }
  }

  const stop = () => {
    if (pollRef.current) clearTimeout(pollRef.current)
    audioRef.current?.pause()
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
    setPhase('idle')
  }

  const toggle = () => {
    if (phase === 'idle') start()
    else stop()
  }

  const title = phase === 'preparing' ? preparingLabel : phase === 'playing' ? stopLabel : label

  return (
    <button
      type="button"
      className={styles.block}
      onClick={toggle}
      aria-pressed={phase !== 'idle'}
      aria-busy={phase === 'preparing'}
    >
      <span className={styles.icon} aria-hidden="true">
        {phase === 'idle' ? (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M8 5v14l11-7z" />
          </svg>
        ) : (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <rect x="6" y="5" width="4" height="14" />
            <rect x="14" y="5" width="4" height="14" />
          </svg>
        )}
      </span>
      <span className={styles.text}>
        <span className={styles.title}>{title}</span>
        {narrator && <span className={styles.narrator}>{narrator}</span>}
      </span>
    </button>
  )
}
