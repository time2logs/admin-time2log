import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['html'], ['list']],

  use: {
    baseURL: process.env.BASE_URL || 'https://dev.admin.time2log.app',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    // Smoke tests - schnelle Checks nach Deployment
    {
      name: 'smoke',
      testMatch: /smoke\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
      retries: 1,
    },

    // Full E2E tests - alle Browser
    {
      name: 'chromium',
      testIgnore: /smoke\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      testIgnore: /smoke\.spec\.ts/,
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      testIgnore: /smoke\.spec\.ts/,
      use: { ...devices['Desktop Safari'] },
    },
  ],
});
