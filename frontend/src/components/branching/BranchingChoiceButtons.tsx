import { useState } from 'react'
import type { BranchingChoice } from '@kazka/shared'
import styles from './BranchingChoiceButtons.module.css'

export interface BranchingChoiceButtonsProps {
  choices: (BranchingChoice | null)[]
  onPick: (choiceId: string) => void
  prompt: string
}

export function BranchingChoiceButtons({ choices, onPick, prompt }: BranchingChoiceButtonsProps) {
  const [picked, setPicked] = useState<string | null>(null)

  function handlePick(id: string) {
    if (picked) return
    setPicked(id)
    onPick(id)
  }

  return (
    <div className={styles.wrap}>
      <p className={styles.prompt}>{prompt}</p>
      <div className={styles.buttons}>
        {choices.map(c => c && (
          <button
            key={c.id}
            type="button"
            className={picked === c.id ? styles.buttonPicked : styles.button}
            disabled={picked !== null}
            onClick={() => handlePick(c.id)}
          >
            {c.text}
          </button>
        ))}
      </div>
    </div>
  )
}
