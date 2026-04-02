import { test, expect } from '@playwright/test';
import {text} from "node:stream/consumers";
import {getRawAsset} from "node:sea";

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

  test('create user', async ({ page }) => {
    await page.goto('http://localhost:4200/auth/register');

    await page.fill('#firstName','testusertime2log@gmail.com');
    await page.fill('#lastName', 'TestUserBLJT2L');
    await page.fill('#email','testusertime2log@gmail.com');
    await page.fill('#password', 'TestUserBLJT2L');
    await page.click('button[type="submit"]');

  })

  test('login content is visible', async ({ page }) => {
    await page.goto('/auth/login');

    await expect(page.locator('app-login')).toBeVisible();
  });

  test('login with valid user', async ({ page }) => {
      await page.goto('http://localhost:4200/auth/login');

        await page.fill('#email','testusertime2log@gmail.com');
        await page.fill('#password', 'TestUserBLJT2L');
        await page.click('button[type="submit"]');

  })

  test('create Organization', async ({ page }) => {
    await page.goto('/auth/login');
    await page.fill('#email', 'testusertime2log@gmail.com');
    await page.fill('#password', 'TestUserBLJT2L');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');

    await page.goto('/organizations');
    await page.getByTestId('organization-name-input').fill('TestOrg');
    await page.getByTestId('create-organization-button').click();
    await expect(page.getByText('TestOrg')).toBeVisible();

    await page.getByText('TestOrg').click();
    await page.getByTestId('back-button').click();
    await expect(page).toHaveURL('/organizations');

    await page.getByText('TestOrg').click();
    await page.getByTestId('open-curriculums-button').click();
    await page.getByTestId('create-profession-button').click();
    await page.getByTestId('profession-key-input').fill('Maurer');
    await page.getByTestId('profession-label-input').fill('Maurer/in EFZ');
    await page.getByTestId('profession-add-button').click();
    await expect(page.getByText('Maurer/in EFZ')).toBeVisible();

    await page.getByTestId('open-teams-button').click();
    await page.getByTestId('create-team-button').click();
    await page.getByTestId('team-name-input').fill('TestTeam');
    await page.getByTestId('team-profession-select').selectOption({ label: 'Maurer/in EFZ'});
    await page.getByTestId('add-team-button').click();
    await expect(page.getByText('TestTeam')).toBeVisible();

    await page.getByTestId('open-settings-button').click();
    await page.getByTestId('delete-organization-button').click();
    await page.getByTestId('confirm-delete-organization-button').click();
    await expect(page).toHaveURL('/organizations');
    await expect(page.getByTestId('organization-name-input')).toBeVisible();
  })



  test('invite cancel', async ({ page }) => {
    await page.goto('/auth/login');
    await page.fill('#email', 'testusertime2log@gmail.com');
    await page.fill('#password', 'TestUserBLJT2L');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');

    await page.goto('/organizations');
    await page.getByTestId('organization-name-input').fill('TestDeleteOrg');
    await page.getByTestId('create-organization-button').click();
    await expect(page.getByText('TestDeleteOrg')).toBeVisible();

    await page.getByText('TestDeleteOrg').click();
    await page.getByTestId('invite-member-button').click();
    await page.getByTestId('email-invite-button').fill('Test@test.com');
    await page.getByTestId('invite-confirm-button').click();

    await page.getByTestId('back-button').click();
    await expect(page).toHaveURL('/organizations');

    await page.getByText('TestDeleteOrg').click();
    await page.getByTestId('cancel-invite-button').click();

    await page.getByTestId('open-settings-button').click();
    await page.getByTestId('delete-organization-button').click();
    await page.getByTestId('confirm-delete-organization-button').click();
    await expect(page).toHaveURL('/organizations');
  })

  test('remove Member', async ({ page }) => {
    await page.goto('/auth/login');
    await page.fill('#email', 'testusertime2log@gmail.com');
    await page.fill('#password', 'TestUserBLJT2L');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');

    await page.goto('/organizations');
    await page.getByText('RemoveMember').click();

    const lukasRow = page.locator('li', { hasText: 'Lukas Traut' });
    await lukasRow.hover();

    await lukasRow.getByTestId('remove-member-button').click();
    await lukasRow.getByTestId('confirm-remove-member-button').click();

    await page.getByTestId('open-settings-button').click();
    await page.getByTestId('delete-organization-button').click();
    await page.getByTestId('confirm-delete-organization-button').click();
    await expect(page).toHaveURL('/organizations');
  });
});






