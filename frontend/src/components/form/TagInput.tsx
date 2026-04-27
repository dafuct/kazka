import { useState, useRef } from 'react'
import type { KeyboardEvent } from 'react'
import styles from './TagInput.module.css'

interface TagInputProps {
  value: string[]
  onChange: (tags: string[]) => void
  placeholder?: string
  id?: string
}

export function TagInput({ value, onChange, placeholder, id }: TagInputProps) {
  const [input, setInput] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  function addTag(text: string) {
    const trimmed = text.trim()
    if (trimmed && !value.includes(trimmed)) {
      onChange([...value, trimmed])
    }
    setInput('')
  }

  function removeTag(index: number) {
    onChange(value.filter((_, i) => i !== index))
  }

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      addTag(input)
    } else if (e.key === 'Backspace' && input === '' && value.length > 0) {
      removeTag(value.length - 1)
    }
  }

  return (
    <div
      className={styles.container}
      onClick={() => inputRef.current?.focus()}
    >
      {value.map((tag, i) => (
        <span key={i} className={styles.tag}>
          {tag}
          <button
            type="button"
            className={styles.remove}
            onClick={(e) => { e.stopPropagation(); removeTag(i) }}
            aria-label={`Видалити ${tag}`}
          >
            ×
          </button>
        </span>
      ))}
      <input
        ref={inputRef}
        id={id}
        type="text"
        className={styles.input}
        value={input}
        placeholder={value.length === 0 ? placeholder : ''}
        onChange={e => setInput(e.target.value)}
        onKeyDown={handleKeyDown}
        onBlur={() => { if (input.trim()) addTag(input) }}
      />
    </div>
  )
}
