import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests of the meal planner in a real browser (030). The API is faked per test with `page.route`
 * (e2e/fakeApi.ts), so no backend or database is needed: `npm run e2e`.
 */
const PORT = 5175;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: `http://localhost:${PORT}`,
    locale: 'vi-VN',
    timezoneId: 'Asia/Ho_Chi_Minh',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    // `SLOWMO=500 npm run e2e -- --headed --workers=1` to watch the browser
    launchOptions: { slowMo: Number(process.env.SLOWMO ?? 0) },
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 1000 } }, grepInvert: /@phone/ },
    { name: 'phone', use: { ...devices['Pixel 5'], viewport: { width: 360, height: 800 } }, grep: /@phone/ },
  ],
  webServer: {
    command: `npx vite --port ${PORT} --strictPort`,
    url: `http://localhost:${PORT}`,
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
});
