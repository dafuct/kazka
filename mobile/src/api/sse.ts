import EventSource from 'react-native-sse';
import { useAuthStore } from '@/src/stores/auth.store';
import { API_BASE_URL } from './config';

export interface SseEvent {
  type: string;
  data: string;
}

type StoryEvent = 'meta' | 'token' | 'done';

/**
 * Opens an SSE connection to a POST endpoint that streams text/event-stream.
 * react-native-sse supports POST + custom headers + body, unlike browser EventSource.
 *
 * Returns: an async iterable of events + a cancel() function.
 * The iterable closes naturally when the server sends a "done" event or
 * after an "error" event that closes the stream.
 */
export function openSseStream(path: string, body: unknown): {
  events: AsyncIterable<SseEvent>;
  cancel: () => void;
} {
  const accessToken = useAuthStore.getState().accessToken;
  const eventSource = new EventSource<StoryEvent>(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    body: JSON.stringify(body),
    pollingInterval: 0, // disable reconnect; story streams are one-shot
  });

  const buffer: SseEvent[] = [];
  const waiters: ((value: IteratorResult<SseEvent>) => void)[] = [];
  let closed = false;

  function push(ev: SseEvent | null) {
    if (waiters.length > 0) {
      const w = waiters.shift()!;
      if (ev === null) w({ value: undefined as unknown as SseEvent, done: true });
      else w({ value: ev, done: false });
    } else {
      if (ev !== null) buffer.push(ev);
    }
  }

  function close() {
    if (closed) return;
    closed = true;
    eventSource.close();
    while (waiters.length > 0) {
      const w = waiters.shift()!;
      w({ value: undefined as unknown as SseEvent, done: true });
    }
  }

  // The backend emits named events: "meta", "token", "done", "error".
  // Plus the default 'message' channel as a fallback, and 'open' for stream open.
  // react-native-sse types data as `string | null` for message/custom events;
  // we coerce to '' for the SseEvent interface.
  ['message', 'meta', 'token', 'done', 'error', 'open'].forEach((type) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    eventSource.addEventListener(type as any, (e: any) => {
      if (type === 'error') {
        // Error union: TimeoutEvent / ExceptionEvent / ErrorEvent — all have `message`.
        const message = e && 'message' in e ? e.message : 'unknown';
        push({ type: 'error', data: message ?? 'unknown' });
        close();
        return;
      }
      push({ type, data: e?.data ?? '' });
      if (type === 'done') close();
    });
  });

  const events: AsyncIterable<SseEvent> = {
    [Symbol.asyncIterator]() {
      return {
        next: () => {
          return new Promise<IteratorResult<SseEvent>>((resolve) => {
            if (buffer.length > 0) {
              const ev = buffer.shift()!;
              resolve({ value: ev, done: false });
            } else if (closed) {
              resolve({ value: undefined as unknown as SseEvent, done: true });
            } else {
              waiters.push(resolve);
            }
          });
        },
        return: () => {
          close();
          return Promise.resolve({ value: undefined as unknown as SseEvent, done: true });
        },
      };
    },
  };

  return { events, cancel: close };
}
