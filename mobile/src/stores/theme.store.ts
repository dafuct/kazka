import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { createMMKV } from 'react-native-mmkv';
import type { VisualStyle, ThemeName } from '@/src/theme/tokens';

const mmkv = createMMKV({ id: 'kazka.theme.v1' });

const zustandMmkvStorage = {
  getItem: (name: string): string | null => mmkv.getString(name) ?? null,
  setItem: (name: string, value: string): void => {
    mmkv.set(name, value);
  },
  removeItem: (name: string): void => {
    mmkv.remove(name);
  },
};

interface ThemeState {
  visualStyle: VisualStyle;
  darkMode: boolean;
  setVisualStyle: (style: VisualStyle) => void;
  setDarkMode: (dark: boolean) => void;
  toggleDarkMode: () => void;
  themeName: () => ThemeName;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      visualStyle: 'cozy',
      darkMode: false,
      setVisualStyle: (visualStyle) => set({ visualStyle }),
      setDarkMode: (darkMode) => set({ darkMode }),
      toggleDarkMode: () => set({ darkMode: !get().darkMode }),
      themeName: () =>
        `${get().visualStyle}${get().darkMode ? 'Dark' : 'Light'}` as ThemeName,
    }),
    {
      name: 'theme',
      storage: createJSONStorage(() => zustandMmkvStorage),
    },
  ),
);
