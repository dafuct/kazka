import '@testing-library/jest-native/extend-expect';

// Mock expo-secure-store with an in-memory map.
jest.mock('expo-secure-store', () => {
  const store = new Map<string, string>();
  return {
    setItemAsync: jest.fn((key: string, value: string) => {
      store.set(key, value);
      return Promise.resolve();
    }),
    getItemAsync: jest.fn((key: string) => Promise.resolve(store.get(key) ?? null)),
    deleteItemAsync: jest.fn((key: string) => {
      store.delete(key);
      return Promise.resolve();
    }),
  };
});

// Mock react-native-mmkv (v4 API: createMMKV factory + remove method).
jest.mock('react-native-mmkv', () => {
  class FakeMMKV {
    private store = new Map<string, string>();
    getString(key: string): string | undefined {
      return this.store.get(key);
    }
    set(key: string, value: string): void {
      this.store.set(key, value);
    }
    remove(key: string): void {
      this.store.delete(key);
    }
    delete(key: string): void {
      this.store.delete(key);
    }
  }
  return {
    MMKV: FakeMMKV,
    createMMKV: (_opts?: { id?: string }) => new FakeMMKV(),
  };
});

// Reset between tests so tests don't leak state.
afterEach(() => {
  const ss = require('expo-secure-store');
  ss.setItemAsync.mockClear();
  ss.getItemAsync.mockClear();
  ss.deleteItemAsync.mockClear();
});
