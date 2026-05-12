import { StyleSheet } from 'react-native-unistyles';
import { themes } from './tokens';

type AppThemes = typeof themes;

// Unistyles 3 reads themes by key and applies the active one on `useStyles`.
// The active theme key is derived from the auth store's visualStyle + dark/light.
StyleSheet.configure({
  themes,
  settings: {
    // The runtime picks the active theme via setTheme(themeName).
    // Default to cozyLight at startup; the AuthGate sets the real value
    // once it reads user preferences.
    initialTheme: 'cozyLight',
  },
});

declare module 'react-native-unistyles' {
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  export interface UnistylesThemes extends AppThemes {}
}
