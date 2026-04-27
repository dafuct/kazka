import type { GenerationRequest } from './types'

export interface SseMetaEvent {
  type: 'meta'
  data: { id: string }
}
export interface SseTokenEvent {
  type: 'token'
  data: { text: string }
}
export interface SseDoneEvent {
  type: 'done'
  data: { id: string; title: string }
}
export interface SseErrorEvent {
  type: 'error'
  data: { message: string }
}

export type SseEvent = SseMetaEvent | SseTokenEvent | SseDoneEvent | SseErrorEvent

export interface SseHandlers {
  onMeta?: (data: SseMetaEvent['data']) => void
  onToken?: (data: SseTokenEvent['data']) => void
  onDone?: (data: SseDoneEvent['data']) => void
  onError?: (data: SseErrorEvent['data']) => void
}

export async function streamStory(
  req: GenerationRequest,
  handlers: SseHandlers,
  signal?: AbortSignal
): Promise<void> {
  const res = await fetch('/api/stories/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(req),
    signal,
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    handlers.onError?.({ message: `HTTP ${res.status}: ${text}` })
    return
  }

  const reader = res.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''

    let eventType = ''
    let dataLine = ''

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventType = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLine = line.slice(5).trim()
      } else if (line === '' && eventType) {
        try {
          const payload = JSON.parse(dataLine)
          const event = { type: eventType, data: payload } as SseEvent
          if (event.type === 'meta') handlers.onMeta?.(event.data)
          else if (event.type === 'token') handlers.onToken?.(event.data)
          else if (event.type === 'done') handlers.onDone?.(event.data)
          else if (event.type === 'error') handlers.onError?.(event.data)
        } catch {
          // ignore malformed lines
        }
        eventType = ''
        dataLine = ''
      }
    }
  }
}
