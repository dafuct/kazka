import { useEffect, useRef, useState } from 'react'
import { narration } from './apiClient'

type Phase = 'idle' | 'preparing' | 'playing' | 'error'

/** Server-side neural narration with Web Speech fallback (moved from the
    retired ReadAloud component). Warm-starts generation on mount (lesson:
    gemini-tts-whole-tale-latency-needs-warm-start); playback needs a tap. */
export function useNarration(storyId: string, text: string, lang = 'uk') {
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
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
    }
  }, [storyId])

  // Warm-start: idempotent server-side; do NOT auto-play (needs user gesture).
  useEffect(() => {
    narration.request(storyId).catch(() => {})
  }, [storyId])

  const fallback = () => {
    // No browser speech synthesis at all → nothing we can do, surface it.
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      setPhase('error')
      return
    }
    const synth = window.speechSynthesis
    const base = lang.slice(0, 2).toLowerCase()
    const voices = synth.getVoices()
    const match = voices.find(v => v.lang?.toLowerCase().startsWith(base))
    // The browser has voices loaded but none for this language (common for uk) →
    // it would "speak" silently. Tell the user instead of pretending to play.
    if (voices.length > 0 && !match) {
      setPhase('error')
      return
    }
    const u = new SpeechSynthesisUtterance(text)
    u.lang = base === 'uk' ? 'uk-UA' : base
    if (match) u.voice = match
    u.rate = 0.96
    u.onend = () => setPhase('idle')
    u.onerror = () => setPhase('error')
    synth.cancel()
    synth.speak(u)
    setPhase('playing')
  }

  const play = (url: string) => {
    if (cancelledRef.current) return
    if (!audioRef.current) {
      audioRef.current = new Audio()
      audioRef.current.onended = () => { setPhase('idle'); setProgress(0) }
      audioRef.current.onerror = () => fallback()
      audioRef.current.ontimeupdate = () => {
        const a = audioRef.current
        if (a && a.duration > 0) setProgress(Math.min(100, (a.currentTime / a.duration) * 100))
      }
    }
    audioRef.current.src = url
    audioRef.current.play().then(() => setPhase('playing')).catch(() => fallback())
  }

  const poll = async () => {
    try {
      const res = await narration.get(storyId)
      if (cancelledRef.current) return
      if (res.status === 'READY' && res.url) play(res.url)
      else if (res.status === 'FAILED') fallback()
      else pollRef.current = setTimeout(poll, 2000)
    } catch {
      fallback()
    }
  }

  const start = async () => {
    setPhase('preparing')
    try {
      const res = await narration.request(storyId)
      if (cancelledRef.current) return
      if (res.status === 'READY' && res.url) play(res.url)
      else if (res.status === 'FAILED') fallback()
      else pollRef.current = setTimeout(poll, 2000)
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
    // idle or a prior error → (re)try; otherwise stop the current playback.
    if (phase === 'idle' || phase === 'error') start()
    else stop()
  }

  return { phase, progress, start, stop, toggle }
}
