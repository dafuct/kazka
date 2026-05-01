import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'

interface Ctx {
  open: boolean
  openModal: () => void
  closeModal: () => void
}

const StoryModalCtx = createContext<Ctx | null>(null)

export function StoryModalProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  return (
    <StoryModalCtx.Provider value={{
      open,
      openModal: () => setOpen(true),
      closeModal: () => setOpen(false),
    }}>
      {children}
    </StoryModalCtx.Provider>
  )
}

export function useStoryModal() {
  const ctx = useContext(StoryModalCtx)
  if (!ctx) throw new Error('useStoryModal must be used within StoryModalProvider')
  return ctx
}
