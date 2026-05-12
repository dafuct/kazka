import { createMMKV } from 'react-native-mmkv';
import type { AsyncStorage } from '@tanstack/query-persist-client-core';

// MMKV-backed AsyncStorage adapter for TanStack Query persistence.
// MMKV is synchronous + fast; we wrap each call in Promise.resolve so the
// AsyncStorage shape (which @tanstack/query-async-storage-persister expects)
// is satisfied.

const mmkv = createMMKV({ id: 'kazka.query.v1' });

export const queryStorage: AsyncStorage<string> = {
  getItem: (key) => Promise.resolve(mmkv.getString(key) ?? null),
  setItem: (key, value) => {
    mmkv.set(key, value);
    return Promise.resolve();
  },
  removeItem: (key) => {
    mmkv.remove(key);
    return Promise.resolve();
  },
};
