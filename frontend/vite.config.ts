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
})
