/// <reference types="vitest" />
import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import basicSsl from '@vitejs/plugin-basic-ssl'

export default defineConfig({
  plugins: [react(), basicSsl()],
  envDir: '..',
  // Monorepo: the mobile/Expo workspace pins react@19.1.0 while this app uses 19.2.x, so npm keeps
  // two react copies (root + frontend/node_modules). Without dedupe, Rolldown bundles two React
  // instances → react-dom's hooks dispatcher is null → "Cannot read properties of null (reading
  // 'useRef')" → blank page in the production build. Force a single instance.
  resolve: {
    dedupe: ['react', 'react-dom'],
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}', 'src/**/__tests__/**/*.{ts,tsx}'],
    // The monorepo has two React copies: root react@19.1.0 (for the Expo workspace)
    // and frontend/node_modules/react@19.2.7. @testing-library/react (root) loads
    // root react. StagePage.tsx (frontend/src/) loads frontend react via Vite. The
    // two instances don't share a dispatcher → "Invalid hook call".
    // alias forces all react/react-dom imports in the Vite module graph to resolve
    // to root's copies, matching @testing-library/react.
    alias: [
      { find: 'react/jsx-dev-runtime', replacement: path.resolve(__dirname, '../node_modules/react/jsx-dev-runtime.js') },
      { find: 'react/jsx-runtime', replacement: path.resolve(__dirname, '../node_modules/react/jsx-runtime.js') },
      { find: 'react-dom/client', replacement: path.resolve(__dirname, '../node_modules/react-dom/client.js') },
      { find: 'react-dom/server', replacement: path.resolve(__dirname, '../node_modules/react-dom/server.js') },
      { find: 'react-dom', replacement: path.resolve(__dirname, '../node_modules/react-dom/index.js') },
      { find: 'react', replacement: path.resolve(__dirname, '../node_modules/react/index.js') },
    ],
  },
})
