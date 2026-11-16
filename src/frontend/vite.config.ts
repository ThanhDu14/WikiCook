/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';
import { mockApi } from './vite.mock-api';

// Dev server proxies the API to the backend so the browser stays same-origin (nginx does the same in Docker).
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080';

  return {
    // `npm run dev:mock` (--mode mock) answers the auth and profile API itself, for looking at the screens without the backend
    plugins: [react(), ...(mode === 'mock' ? [mockApi()] : [])],
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': apiTarget,
        '/actuator': apiTarget,
      },
    },
    test: {
      environment: 'happy-dom',
      globals: true,
      setupFiles: ['./src/test/setup.ts'],
      css: false,
      // Playwright specs run with `npm run e2e`
      exclude: ['e2e/**', 'node_modules/**'],
    },
  };
});
