import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/login/',
  publicDir: false,
  build: {
    outDir: 'build/dist/login',
    emptyOutDir: true,
    rollupOptions: {
      input: 'login.html',
    },
  },
})
