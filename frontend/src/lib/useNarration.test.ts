import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useNarration } from './useNarration'
import { narration } from './apiClient'

vi.mock('./apiClient', () => ({
  narration: { request: vi.fn(), get: vi.fn() },
}))

describe('useNarration', () => {
  let playSpy: ReturnType<typeof vi.fn>
  let speakSpy: ReturnType<typeof vi.fn>

  beforeEach(() => {
    playSpy = vi.fn().mockResolvedValue(undefined)
    window.HTMLMediaElement.prototype.play = playSpy as never
    window.HTMLMediaElement.prototype.pause = vi.fn() as never

    // A "healthy" Web Speech API — present and armed with voices. Old code would
    // happily speak through this; new code must never touch it at all.
    speakSpy = vi.fn()
    vi.stubGlobal('speechSynthesis', { speak: speakSpy, cancel: vi.fn(), getVoices: () => [] })
    vi.stubGlobal('SpeechSynthesisUtterance', class {
      lang = ''
      rate = 1
      voice: unknown = null
      onend: (() => void) | null = null
      onerror: (() => void) | null = null
      constructor(public text: string) {}
    } as never)
  })

  afterEach(() => {
    vi.clearAllMocks()
    vi.unstubAllGlobals()
    vi.useRealTimers()
  })

  it('sets phase to error (not playing) when start() resolves FAILED, and never speaks', async () => {
    vi.mocked(narration.request).mockResolvedValue({ status: 'FAILED', url: null })

    const { result } = renderHook(() => useNarration('story-1', 'Жила собі лисичка.', 'uk'))
    await act(async () => { await result.current.start() })

    await waitFor(() => expect(result.current.phase).toBe('error'))
    expect(result.current.phase).not.toBe('playing')
    expect(speakSpy).not.toHaveBeenCalled()
  })

  it('sets phase to error when start() rejects (network error), and never speaks', async () => {
    vi.mocked(narration.request).mockRejectedValue(new Error('network'))

    const { result } = renderHook(() => useNarration('story-1', 'Жила собі лисичка.', 'uk'))
    await act(async () => { await result.current.start() })

    await waitFor(() => expect(result.current.phase).toBe('error'))
    expect(speakSpy).not.toHaveBeenCalled()
  })

  it('sets phase to error when polling resolves FAILED, and never speaks', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    vi.mocked(narration.request).mockResolvedValue({ status: 'GENERATING', url: null })
    vi.mocked(narration.get).mockResolvedValue({ status: 'FAILED', url: null })

    const { result } = renderHook(() => useNarration('story-1', 'Жила собі лисичка.', 'uk'))
    await act(async () => { await result.current.start() })
    expect(result.current.phase).toBe('preparing')

    await act(async () => { await vi.advanceTimersByTimeAsync(2000) })

    expect(result.current.phase).toBe('error')
    expect(speakSpy).not.toHaveBeenCalled()
  })

  it('sets phase to error when a poll request throws (network error), and never speaks', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    vi.mocked(narration.request).mockResolvedValue({ status: 'GENERATING', url: null })
    vi.mocked(narration.get).mockRejectedValue(new Error('network'))

    const { result } = renderHook(() => useNarration('story-1', 'Жила собі лисичка.', 'uk'))
    await act(async () => { await result.current.start() })
    expect(result.current.phase).toBe('preparing')

    await act(async () => { await vi.advanceTimersByTimeAsync(2000) })

    expect(result.current.phase).toBe('error')
    expect(speakSpy).not.toHaveBeenCalled()
  })

  it('sets phase to error when the audio element fails to play, and never speaks', async () => {
    vi.mocked(narration.request).mockResolvedValue({ status: 'READY', url: '/uploads/story-1.wav' })
    playSpy.mockRejectedValue(new Error('NotSupportedError'))

    const { result } = renderHook(() => useNarration('story-1', 'Жила собі лисичка.', 'uk'))
    await act(async () => { await result.current.start() })

    await waitFor(() => expect(result.current.phase).toBe('error'))
    expect(speakSpy).not.toHaveBeenCalled()
  })

  it('retries via toggle() after an error and can reach playing on a subsequent success, without ever speaking', async () => {
    vi.mocked(narration.request).mockResolvedValue({ status: 'FAILED', url: null })

    const { result } = renderHook(() => useNarration('story-1', 'Жила собі лисичка.', 'uk'))
    await act(async () => { await result.current.start() })
    await waitFor(() => expect(result.current.phase).toBe('error'))

    vi.mocked(narration.request).mockResolvedValue({ status: 'READY', url: '/uploads/story-1.wav' })
    await act(async () => { result.current.toggle() })

    await waitFor(() => expect(playSpy).toHaveBeenCalled())
    expect(result.current.phase).toBe('playing')
    expect(speakSpy).not.toHaveBeenCalled()
  })
})
