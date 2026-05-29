import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
    rules: {
      // Pervasive in API/error-handling code; keep visible as a warning, not a build break.
      '@typescript-eslint/no-explicit-any': 'warn',
      // react-hooks 7 flags data-fetching effects (setLoading before fetch); intentional pattern here.
      'react-hooks/set-state-in-effect': 'warn',
      // Context files co-locate Provider + use* hook by design.
      'react-refresh/only-export-components': 'warn',
    },
  },
])
