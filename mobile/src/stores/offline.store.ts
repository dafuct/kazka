import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

// Match M4/M5 MMKV pattern (v4: createMMKV factory + .remove).
function makeMmkv() {
  const mod = require('react-native-mmkv');
  return mod.createMMKV
    ? mod.createMMKV({ id: 'kazka.offline.v1' })
    : new mod.MMKV({ id: 'kazka.offline.v1' });
}

const mmkv = makeMmkv();

const zustandMmkvStorage = {
  getItem: (name: string): string | null => mmkv.getString(name) ?? null,
  setItem: (name: string, value: string): void => { mmkv.set(name, value); },
  removeItem: (name: string): void => {
    if (typeof mmkv.remove === 'function') mmkv.remove(name);
    else mmkv.delete(name);
  },
};

export type QueuedOp =
  | { kind: 'rename'; id: string; title: string; content: string }
  | { kind: 'delete'; id: string };

interface OfflineState {
  queue: QueuedOp[];
  enqueue: (op: QueuedOp) => void;
  drainOne: () => QueuedOp | undefined;
  clear: () => void;
}

export const useOfflineStore = create<OfflineState>()(
  persist(
    (set, get) => ({
      queue: [],
      enqueue: (op) => set({ queue: [...get().queue, op] }),
      drainOne: () => {
        const q = get().queue;
        if (q.length === 0) return undefined;
        const [head, ...rest] = q;
        set({ queue: rest });
        return head;
      },
      clear: () => set({ queue: [] }),
    }),
    {
      name: 'offline-queue',
      storage: createJSONStorage(() => zustandMmkvStorage),
    },
  ),
);
