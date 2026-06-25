import { test, expect } from '@playwright/test';

test.describe('Smoke Tests', () => {
  test('app is reachable and loads', async ({page}) => {
    const response = await page.goto('/');

    expect(response?.status()).toBe(200);
    await expect(page).toHaveTitle(/.+/);
  });

  test('no console errors on load', async ({page}) => {
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

  test('main content is visible', async ({page}) => {
    await page.goto('/');

    // App-Root sollte existieren
    await expect(page.locator('app-root')).toBeVisible();
  });

  test('register content is visible', async ({page}) => {
    await page.goto('/auth/register');

    await expect(page.locator('app-register')).toBeVisible();
  });

  test('create user', async ({page}) => {
    await page.goto('http://localhost:4200/auth/register');

    await page.fill('#firstName', 'testusertime2log@gmail.com');
    await page.fill('#lastName', 'TestUserBLJT2L');
    await page.fill('#email', 'testusertime2log@gmail.com');
    await page.fill('#password', 'TestUserBLJT2L');
    await page.click('button[type="submit"]');

  })

  test('login content is visible', async ({page}) => {
    await page.goto('/auth/login');

    await expect(page.locator('app-login')).toBeVisible();
  });

  test('login with valid user', async ({page}) => {
    await page.goto('http://localhost:4200/auth/login');

    await page.fill('#email', 'testusertime2log@gmail.com');
    await page.fill('#password', 'TestUserBLJT2L');
    await page.click('button[type="submit"]');

  })

  test('create Organization', async ({page}) => {
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
    await page.getByTestId('team-profession-select').selectOption({label: 'Maurer/in EFZ'});
    await page.getByTestId('add-team-button').click();
    await expect(page.getByText('TestTeam')).toBeVisible();

    await page.getByTestId('open-settings-button').click();
    await page.getByTestId('delete-organization-button').click();
    await page.getByTestId('confirm-delete-organization-button').click();
    await expect(page).toHaveURL('/organizations');
    await expect(page.getByTestId('organization-name-input')).toBeVisible();
  })


  test('invite cancel', async ({page}) => {
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

  test('remove member', async ({page}) => {
    await page.goto('/auth/login');
    await page.fill('#email', 'testusertime2log@gmail.com');
    await page.fill('#password', 'TestUserBLJT2L');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');

    await page.goto('/organizations');
    await page.getByTestId('organization-name-input').fill('RemoveMemberTestOrg');
    await page.getByTestId('create-organization-button').click();
    await page.getByText('RemoveMemberTestOrg').first().click();

    // Owner sollte keinen Remove-Button haben
    const memberRows = page.locator('[data-testid="open-member-report-button"]');
    await expect(memberRows.first()).toBeVisible();
    await memberRows.first().hover();
    await expect(page.getByTestId('remove-member-button')).not.toBeVisible();

    // cleanup
    await page.getByTestId('open-settings-button').click();
    await page.getByTestId('delete-organization-button').click();
    await page.getByTestId('confirm-delete-organization-button').click();
    await expect(page).toHaveURL('/organizations');
  });


  const LOGIN_EMAIL = 'testusertime2log@gmail.com';
  const LOGIN_PASSWORD = 'TestUserBLJT2L';

  async function login(page: any) {
    await page.goto('/auth/login');
    await page.fill('#email', LOGIN_EMAIL);
    await page.fill('#password', LOGIN_PASSWORD);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');
  }

// ─── DASHBOARD ───────────────────────────────────────────────────────────────

  test.describe('Dashboard', () => {
    test('dashboard is visible after login', async ({page}) => {
      await login(page);
      await expect(page.locator('app-root')).toBeVisible();
    });

    test('inactive members section is visible', async ({page}) => {
      await login(page);
      await page.goto('/dashboard');
      await expect(page.locator('select').first()).toBeVisible();
    });

    test('inactive days filter can be changed', async ({page}) => {
      await login(page);
      await page.goto('/dashboard');
      const select = page.locator('select').first();
      await select.selectOption('7');
      await expect(select).toHaveValue('7');
      await select.selectOption('30');
      await expect(select).toHaveValue('30');
    });
  });

// ─── ORGANIZATION MANAGING ───────────────────────────────────────────────────

  test.describe('Organization Managing', () => {
    test('back button navigates to organizations', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('NavTestOrg');
      await page.getByTestId('create-organization-button').click();
      await expect(page.getByText('NavTestOrg')).toBeVisible();
      await page.getByText('NavTestOrg').click();
      await page.getByTestId('back-button').click();
      await expect(page).toHaveURL('/organizations');

      // cleanup
      await page.getByText('NavTestOrg').click();
      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });

    test('tabs are switchable', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('TabTestOrg');
      await page.getByTestId('create-organization-button').click();
      await page.getByText('TabTestOrg').click();

      await page.getByTestId('open-curriculums-button').click();
      await expect(page.getByTestId('create-profession-button')).toBeVisible();

      await page.getByTestId('open-teams-button').click();
      await expect(page.getByTestId('create-team-button')).toBeVisible();

      await page.getByTestId('open-settings-button').click();
      await expect(page.getByTestId('delete-organization-button')).toBeVisible();

      await page.getByTestId('open-members-button').click();
      await expect(page.getByTestId('invite-member-button')).toBeVisible();

      // cleanup
      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });

    test('invite form toggles open and closed', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('InviteToggleOrg');
      await page.getByTestId('create-organization-button').click();
      await page.getByText('InviteToggleOrg').click();

      await page.getByTestId('invite-member-button').click();
      await expect(page.getByTestId('email-invite-button')).toBeVisible();
      await page.getByTestId('invite-member-button').click();
      await expect(page.getByTestId('email-invite-button')).not.toBeVisible();

      // cleanup
      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });

    test('invite button is disabled without email', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('InviteDisabledOrg');
      await page.getByTestId('create-organization-button').click();
      await page.getByText('InviteDisabledOrg').click();

      await page.getByTestId('invite-member-button').click();
      await expect(page.getByTestId('invite-confirm-button')).toBeDisabled();

      // cleanup
      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });

    test('profession can be created and is visible', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('ProfessionTestOrg');
      await page.getByTestId('create-organization-button').click();
      await page.getByText('ProfessionTestOrg').click();

      await page.getByTestId('open-curriculums-button').click();
      await page.getByTestId('create-profession-button').click();
      await page.getByTestId('profession-key-input').fill('informatiker');
      await page.getByTestId('profession-label-input').fill('Informatiker EFZ');
      await page.getByTestId('profession-add-button').click();
      await expect(page.getByText('Informatiker EFZ')).toBeVisible();

      // cleanup
      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });

    test('profession add button is disabled without inputs', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('ProfessionDisabledOrg');
      await page.getByTestId('create-organization-button').click();
      await page.getByText('ProfessionDisabledOrg').click();

      await page.getByTestId('open-curriculums-button').click();
      await page.getByTestId('create-profession-button').click();
      await expect(page.getByTestId('profession-add-button')).toBeDisabled();

      // cleanup
      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });

    test('team can be created and is visible', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('TeamTestOrg');
      await page.getByTestId('create-organization-button').click();
      await page.getByText('TeamTestOrg').click();

      await page.getByTestId('open-curriculums-button').click();
      await page.getByTestId('create-profession-button').click();
      await page.getByTestId('profession-key-input').fill('elektriker');
      await page.getByTestId('profession-label-input').fill('Elektriker EFZ');
      await page.getByTestId('profession-add-button').click();

      await page.getByTestId('open-teams-button').click();
      await page.getByTestId('create-team-button').click();
      await page.getByTestId('team-name-input').fill('Team Alpha');
      await page.getByTestId('team-profession-select').selectOption({label: 'Elektriker EFZ'});
      await page.getByTestId('add-team-button').click();
      await expect(page.getByText('Team Alpha')).toBeVisible();

      // cleanup
      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });

    test('team can be deleted', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('TeamDeleteOrg');
      await page.getByTestId('create-organization-button').click();
      await page.getByText('TeamDeleteOrg').click();

      await page.getByTestId('open-curriculums-button').click();
      await page.getByTestId('create-profession-button').click();
      await page.getByTestId('profession-key-input').fill('schreiner');
      await page.getByTestId('profession-label-input').fill('Schreiner EFZ');
      await page.getByTestId('profession-add-button').click();

      await page.getByTestId('open-teams-button').click();
      await page.getByTestId('create-team-button').click();
      await page.getByTestId('team-name-input').fill('Team Beta');
      await page.getByTestId('team-profession-select').selectOption({label: 'Schreiner EFZ'});
      await page.getByTestId('add-team-button').click();

      const teamRow = page.locator('li', {hasText: 'Team Beta'});
      await teamRow.hover();
      await teamRow.getByTestId('delete-team-button').click();
      await teamRow.getByTestId('confirm-delete-team-button').click();
      await expect(page.getByText('Team Beta')).not.toBeVisible();

      // cleanup
      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });

    test('delete organization cancel works', async ({page}) => {
      await login(page);
      await page.goto('/organizations');
      await page.getByTestId('organization-name-input').fill('CancelDeleteOrg');
      await page.getByTestId('create-organization-button').click();
      await page.getByText('CancelDeleteOrg').click();

      await page.getByTestId('open-settings-button').click();
      await page.getByTestId('delete-organization-button').click();
      await expect(page.getByTestId('confirm-delete-organization-button')).toBeVisible();
      await page.getByTestId('cancel-delete-organization-button').click();
      await expect(page.getByTestId('confirm-delete-organization-button')).not.toBeVisible();

      // cleanup
      await page.getByTestId('delete-organization-button').click();
      await page.getByTestId('confirm-delete-organization-button').click();
    });
  });

// ─── SETTINGS ────────────────────────────────────────────────────────────────

  test.describe('Settings', () => {
    test('settings page is reachable', async ({page}) => {
      await login(page);
      await page.goto('/settings');
      await expect(page.locator('app-root')).toBeVisible();
    });

    test('user profile is visible', async ({page}) => {
      await login(page);
      await page.goto('/settings');
      await expect(page.locator('code')).toBeVisible();
    });

    test('email change input is visible', async ({page}) => {
      await login(page);
      await page.goto('/settings');
      await expect(page.locator('input[type="email"]')).toBeVisible();
    });

    test('email submit is disabled with invalid email', async ({page}) => {
      await login(page);
      await page.goto('/settings');
      await page.locator('input[type="email"]').fill('kein-email');
      await expect(page.locator('button[type="submit"]')).toBeDisabled();
    });

    test('delete account confirm flow works', async ({page}) => {
      await login(page);
      await page.goto('/settings');
      await page.locator('button', {hasText: /löschen|delete/i}).first().click();
      await expect(page.locator('button', {hasText: /bestätigen|confirm|delete/i}).last()).toBeVisible();
      await page.locator('button', {hasText: /abbrechen|cancel/i}).click();
      await expect(page.locator('button', {hasText: /löschen|delete/i}).first()).toBeVisible();
    });
  });
});

