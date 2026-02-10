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

  test('register content is visible', async ({ page }) => {
      await page.goto('/auth/register');

      await expect(page.locator('app-register')).toBeVisible();
  });

  test('login content is visible', async ({ page }) => {
    await page.goto('/auth/login');

    await expect(page.locator('app-login')).toBeVisible();
  });

  test('login with valid user', async ({ page }) => {
      await page.goto('/auth/login');

      /* Test User Data:
         Name: TestUser1
         Email: testusertime2log@gmail.com
         PW: TestUserBLJT2L
       */

        await page.fill('#email','testusertime2log@gmail.com');
        await page.fill('#password', 'TestUserBLJT2L');
        await page.click('button[type="submit"]');

      await expect(page).toHaveURL('/dashboard');


  })

});
