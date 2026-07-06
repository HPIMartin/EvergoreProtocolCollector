/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'build/dist',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    reporters: ['default', 'junit'],
    outputFile: 'build/reports/vitest/results.xml',
  },
})
