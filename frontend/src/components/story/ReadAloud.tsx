import { useEffect, useRef, useState } from 'react'
import styles from './ReadAloud.module.css'

interface ReadAloudProps {
  text: string
  /** BCP-47 language of the story text, e.g. "uk" or "en" */
  lang?: string
  label: string
  stopLabel: string
  narrator?: string
}

/**
 * Best-effort read-aloud using the browser Web Speech API (SpeechSynthesis).
 * Renders nothing if the browser doesn't support it. Picks a voice matching
 * the story language when the OS provides one.
 */
export function ReadAloud({ text, lang = 'uk', label, stopLabel, narrator }: ReadAloudProps) {
  const supported =
    typeof window !== 'undefined' && 'speechSynthesis' in window
  const [speaking, setSpeaking] = useState(false)
  const utterRef = useRef<SpeechSynthesisUtterance | null>(null)

  useEffect(() => {
    if (!supported) return
    return () => window.speechSynthesis.cancel()
  }, [supported])

  // Stop if the text changes (navigated to another story)
  useEffect(() => {
    if (!supported) return
    window.speechSynthesis.cancel()
    setSpeaking(false)
  }, [text, supported])

  if (!supported) return null

  const toggle = () => {
    const synth = window.speechSynthesis
    if (speaking) {
      synth.cancel()
      setSpeaking(false)
      return
    }
    const u = new SpeechSynthesisUtterance(text)
    const base = lang.slice(0, 2).toLowerCase()
    u.lang = base === 'uk' ? 'uk-UA' : base
    const voices = synth.getVoices()
    const match = voices.find((v) => v.lang?.toLowerCase().startsWith(base))
    if (match) u.voice = match
    u.rate = 0.96
    u.pitch = 1
    u.onend = () => setSpeaking(false)
    u.onerror = () => setSpeaking(false)
    utterRef.current = u
    synth.cancel()
    synth.speak(u)
    setSpeaking(true)
  }

  return (
    <button
      type="button"
      className={styles.block}
      onClick={toggle}
      aria-pressed={speaking}
    >
      <span className={styles.icon} aria-hidden="true">
        {speaking ? (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <rect x="6" y="5" width="4" height="14" />
            <rect x="14" y="5" width="4" height="14" />
          </svg>
        ) : (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M8 5v14l11-7z" />
          </svg>
        )}
      </span>
      <span className={styles.text}>
        <span className={styles.title}>{speaking ? stopLabel : label}</span>
        {narrator && <span className={styles.narrator}>{narrator}</span>}
      </span>
    </button>
  )
}
