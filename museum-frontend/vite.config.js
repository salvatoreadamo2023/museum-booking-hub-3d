import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // Il backend accetta richieste (CORS) solo da http://localhost:5173
  server: { port: 5173, strictPort: true },
  // three.js da solo supera i 500 kB: è in un file a parte che si scarica solo aprendo la mappa
  build: { chunkSizeWarningLimit: 700 },
})
