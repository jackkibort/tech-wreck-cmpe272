import { expect, type Page } from '@playwright/test';
import { TOTP } from 'otpauth';
import { passwordFor, totpSecretFor } from './env';

/** Keycloak's login page fields (standard keycloak.v2 theme ids). */
const KC = {
  username: '#username',
  password: '#password',
  submit: '#kc-login',
  otp: '#otp',
  error: '#input-error, #input-error-username, .kc-feedback-text, [id^="input-error"]',
} as const;

/** Current one-time code for an MFA user (Keycloak's default: SHA1, 6 digits, 30 seconds). */
export function currentOtp(username: string): string {
  return new TOTP({ secret: totpSecretFor(username), digits: 6, period: 30, algorithm: 'SHA1' }).generate();
}

/** Fills Keycloak's login form. The page must already be on Keycloak's sign-in screen. */
export async function submitCredentials(page: Page, username: string, password: string): Promise<void> {
  await page.locator(KC.username).fill(username);
  await page.locator(KC.password).fill(password);
  await page.locator(KC.submit).click();
}

/**
 * Signs in through Keycloak from wherever the app sent the browser, answering the one-time code
 * prompt when Keycloak asks for it (HR_ADMIN and AUDITOR, Section 0.5).
 */
export async function signIn(page: Page, username: string, options: { mfa?: boolean } = {}): Promise<void> {
  await expect(page.locator(KC.username)).toBeVisible();
  await submitCredentials(page, username, passwordFor(username));
  if (options.mfa) {
    await expect(page.locator(KC.otp)).toBeVisible();
    await page.locator(KC.otp).fill(currentOtp(username));
    await page.locator(KC.submit).click();
  }
}

/** Opens the portal and signs in; resolves once the portal shows who is signed in. */
export async function signInToPortal(page: Page, username: string, options: { mfa?: boolean } = {}): Promise<void> {
  await page.goto('/');
  await signIn(page, username, options);
  await expect(page.getByLabel('Signed in as')).toContainText(username);
}

export { KC };
