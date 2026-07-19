import { useEffect, useRef, useState } from 'react'
import { narration } from './apiClient'

type Phase = 'idle' | 'preparing' | 'playing' | 'error'

/** Server-side neural narration only — no Web Speech fallback. Warm-starts
    generation on mount (lesson: gemini-tts-whole-tale-latency-needs-warm-start);
    playback needs a tap. Any failure (request, poll, or audio playback) surfaces
    honestly as phase 'error' instead of silently degrading to the browser's
    robotic voice; the caller can retry via toggle()/start(). */
// text/lang are unused now that the Web Speech fallback (their only consumer) is gone;
// kept for signature stability with the ReaderAudioBar call site.
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function useNarration(storyId: string, _text: string, _lang = 'uk') {
  const [phase, setPhase] = useState<Phase>('idle')
  const [progress, setProgress] = useState(0) // 0..100 of the audio clip
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const pollRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const cancelledRef = useRef(false)

  useEffect(() => {
    cancelledRef.current = false
    return () => {
      cancelledRef.current = true
      if (pollRef.current) clearTimeout(pollRef.current)
      audioRef.current?.pause()
    }
  }, [storyId])

  // Warm-start: idempotent server-side; do NOT auto-play (needs user gesture).
  useEffect(() => {
    narration.request(storyId).catch(() => {})
  }, [storyId])

  const play = (url: string) => {
    if (cancelledRef.current) return
    if (!audioRef.current) {
      audioRef.current = new Audio()
      audioRef.current.onended = () => { setPhase('idle'); setProgress(0) }
      audioRef.current.onerror = () => setPhase('error')
      audioRef.current.ontimeupdate = () => {
        const a = audioRef.current
        if (a && a.duration > 0) setProgress(Math.min(100, (a.currentTime / a.duration) * 100))
      }
    }
    audioRef.current.src = url
    audioRef.current.play().then(() => setPhase('playing')).catch(() => setPhase('error'))
  }

  const poll = async () => {
    try {
      const res = await narration.get(storyId)
      if (cancelledRef.current) return
      if (res.status === 'READY' && res.url) play(res.url)
      else if (res.status === 'FAILED') setPhase('error')
      else pollRef.current = setTimeout(poll, 2000)
    } catch {
      setPhase('error')
    }
  }

  const start = async () => {
    setPhase('preparing')
    try {
      const res = await narration.request(storyId)
      if (cancelledRef.current) return
      if (res.status === 'READY' && res.url) play(res.url)
      else if (res.status === 'FAILED') setPhase('error')
      else pollRef.current = setTimeout(poll, 2000)
    } catch {
      setPhase('error')
    }
  }

  const stop = () => {
    if (pollRef.current) clearTimeout(pollRef.current)
    audioRef.current?.pause()
    setPhase('idle')
  }

  const toggle = () => {
    // idle or a prior error → (re)try; otherwise stop the current playback.
    if (phase === 'idle' || phase === 'error') start()
    else stop()
  }

  return { phase, progress, start, stop, toggle }
}
