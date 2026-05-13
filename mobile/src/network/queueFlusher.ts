import NetInfo from '@react-native-community/netinfo';
import { useOfflineStore, type QueuedOp } from '@/src/stores/offline.store';
import { storiesApi } from '@/src/api/stories';

let flushing = false;

async function applyOne(op: QueuedOp): Promise<void> {
  if (op.kind === 'rename') {
    await storiesApi.update(op.id, { title: op.title, content: op.content });
  } else if (op.kind === 'delete') {
    await storiesApi.remove(op.id);
  }
}

async function flushAll(): Promise<void> {
  if (flushing) return;
  flushing = true;
  try {
    while (useOfflineStore.getState().queue.length > 0) {
      const head = useOfflineStore.getState().queue[0];
      if (!head) break;
      try {
        await applyOne(head);
        useOfflineStore.getState().drainOne();
      } catch {
        // First failure — stop draining; leave the rest queued for next reconnect.
        break;
      }
    }
  } finally {
    flushing = false;
  }
}

/**
 * Subscribes to NetInfo. Whenever we transition to online, attempt to flush
 * the offline queue. Returns an unsubscribe function.
 */
export function subscribeToQueueFlush(): () => void {
  const sub = NetInfo.addEventListener((s) => {
    if (s.isConnected) void flushAll();
  });
  return sub;
}
