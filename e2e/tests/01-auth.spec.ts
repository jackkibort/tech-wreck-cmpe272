import { expect, test } from '@playwright/test';
import { demo, urls } from '../support/env';
import { KC, signIn, signInToPortal } from '../support/login';

// Single sign-on (Sections 0.5 and 5.1; use case UC-01).

test('TC-AUTH-01 directory user signs in through SSO and reaches the portal', async ({ page }) => {
  await page.goto('/');
  // The portal has no token, so it sends the browser to Keycloak first.
  await expect(page).toHaveURL(/\/realms\/securehr\/protocol\/openid-connect\/auth/);
  await signIn(page, demo.employee.username);
  await expect(page).toHaveURL(/\/directory$/);
  await expect(page.getByLabel('Signed in as')).toHaveText(demo.employee.username);
});

test('TC-AUTH-02 HR Admin is asked for a one-time code', async ({ page }) => {
  await page.goto('/');
  await signIn(page, demo.hradmin.username, { mfa: true });
  await expect(page.getByLabel('Signed in as')).toHaveText(demo.hradmin.username);
});

test('TC-AUTH-03 one login opens Jenkins and Grafana for a dev user', async ({ page }) => {
  test.skip(!demo.dev.username, 'Set E2E_DEV_USERNAME to a securehr-devs account');
  await page.goto(urls.jenkins);
  await signIn(page, demo.dev.username);
  await expect(page).toHaveURL(new URL(urls.jenkins).origin + '/');

  // The Keycloak session is reused: Grafana opens with no second login form.
  await page.goto(urls.grafana);
  await expect(page).toHaveURL(new RegExp('^' + new URL(urls.grafana).origin));
  await expect(page.locator(KC.username)).toHaveCount(0);
});

test('TC-AUTH-04 signing out ends the session across apps', async ({ page }) => {
  await signInToPortal(page, demo.employee.username);
  await page.getByRole('button', { name: 'Sign out' }).click();

  // Back on the portal, Keycloak asks for credentials again: the SSO session is gone.
  await page.goto('/');
  await expect(page.locator(KC.username)).toBeVisible();
  // The same holds for the tools that share the session.
  await page.goto(urls.grafana);
  await expect(page.locator(KC.username)).toBeVisible();
});
