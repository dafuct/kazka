import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ReaderAudioBar } from './ReaderAudioBar'
import { narration } from '../../lib/apiClient'

vi.mock('../../lib/apiClient', () => ({
  narration: { request: vi.fn(), get: vi.fn() },
}))

describe('ReaderAudioBar', () => {
  let playSpy: ReturnType<typeof vi.fn>

  beforeEach(() => {
    playSpy = vi.fn().mockResolvedValue(undefined)
    window.HTMLMediaElement.prototype.play = playSpy as never
    window.HTMLMediaElement.prototype.pause = vi.fn() as never
  })
  afterEach(() => vi.clearAllMocks())

  const props = {
    storyId: 'story-1',
    text: 'Жила собі лисичка.',
    lang: 'uk',
    label: 'Слухати озвучку',
    stopLabel: 'Зупинити',
    preparingLabel: 'Готую озвучення…',
  }

  it('plays the returned audio url when narration is READY', async () => {
    vi.mocked(narration.request).mockResolvedValue({ status: 'READY', url: '/uploads/story-1.wav' })

    render(<ReaderAudioBar {...props} />)
    fireEvent.click(screen.getByRole('button'))

    await waitFor(() => expect(playSpy).toHaveBeenCalled())
    expect(narration.request).toHaveBeenCalledWith('story-1')
  })

  it('warm-starts narration generation on mount without auto-playing', async () => {
    vi.mocked(narration.request).mockResolvedValue({ status: 'GENERATING', url: null })

    render(<ReaderAudioBar {...props} />)

    await waitFor(() => expect(narration.request).toHaveBeenCalledWith('story-1'))
    expect(playSpy).not.toHaveBeenCalled()
  })

  it('falls back to speechSynthesis when the narration request fails', async () => {
    vi.mocked(narration.request).mockRejectedValue(new Error('network'))
    const speak = vi.fn()
    vi.stubGlobal('speechSynthesis', { speak, cancel: vi.fn(), getVoices: () => [] })
    vi.stubGlobal('SpeechSynthesisUtterance', class { lang = ''; rate = 1; pitch = 1; onend = null; onerror = null } as never)

    render(<ReaderAudioBar {...props} />)
    fireEvent.click(screen.getByRole('button'))

    await waitFor(() => expect(speak).toHaveBeenCalled())
    vi.unstubAllGlobals()
  })
})
