import { expect, test } from '@playwright/test';
import { demo, passwordFor } from '../support/env';
import { KC, submitCredentials } from '../support/login';

// Misuse cases (use case UC-10; scenario S5).

test('TC-E2E-06 five wrong passwords lock the account', async ({ page }) => {
  const username = demo.employee2.username;
  await page.goto('/');
  for (let attempt = 1; attempt <= 5; attempt += 1) {
    await submitCredentials(page, username, `wrong-password-${attempt}`);
    await expect(page.locator(KC.username)).toBeVisible();
  }

  // Locked: even the right password is refused now (Section 8.3, brute-force protection).
  await submitCredentials(page, username, passwordFor(username));
  await expect(page.locator(KC.username)).toBeVisible();
  await expect(page.getByLabel('Signed in as')).toHaveCount(0);
  // The reset procedure (root README) unlocks demo.employee2 before the next demo.
});

// GitHub blocks automated sign-in, so this case is run by hand (Section 20); the steps and
// screenshots are in /e2e/README.md and /docs/evidence/e2e/.
test.skip('TC-E2E-07 unlinked GitHub login gets no access (manual test)', () => {});
