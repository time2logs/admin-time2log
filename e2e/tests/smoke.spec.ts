import { test, expect } from '@playwright/test';

test.describe('Smoke Tests', () => {
  test('app is reachable and loads', async ({ page }) => {
    const response = await page.goto('/');

    expect(response?.status()).toBe(200);
    await expect(page).toHaveTitle(/.+/);
  });

  test('no console errors on load', async ({ page }) => {
    const errors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    expect(errors).toHaveLength(0);
  });

  test('main content is visible', async ({ page }) => {
    await page.goto('/');

    // App-Root sollte existieren
    await expect(page.locator('app-root')).toBeVisible();
  });
});
