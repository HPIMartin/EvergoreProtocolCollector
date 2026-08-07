/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const RUNNING_APPLICATION = 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': RUNNING_APPLICATION,
    },
  },
  build: {
    outDir: 'build/dist',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    maxWorkers: 2,
    reporters: ['default', 'junit'],
    outputFile: 'build/reports/vitest/results.xml',
  },
})
